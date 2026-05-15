package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.collection.ConceptCleanseService;
import com.tradingdiary.service.collection.IndustryCleanseService;
import com.tradingdiary.service.collection.MarginCleanseService;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import com.tradingdiary.service.collection.StockInfoCleanseService;
import com.tradingdiary.service.collection.TradeCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CollectionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CollectionOrchestrator.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final AKToolsClient aktoolsClient;
    private final DataCollectionLogMapper logMapper;
    private final RawDataMapper rawDataMapper;
    private final StockInfoCleanseService stockInfoCleanseService;
    private final StockDailyCleanseService stockDailyCleanseService;
    private final IndustryCleanseService industryCleanseService;
    private final ConceptCleanseService conceptCleanseService;
    private final MarginCleanseService marginCleanseService;
    private final TradeCalendarService tradeCalendarService;
    private final TradeCalendarMapper tradeCalendarMapper;
    private final IndustryMapper industryMapper;
    private final ConceptMapper conceptMapper;

    public CollectionOrchestrator(AKToolsClient aktoolsClient,
                                  DataCollectionLogMapper logMapper,
                                  RawDataMapper rawDataMapper,
                                  StockInfoCleanseService stockInfoCleanseService,
                                  StockDailyCleanseService stockDailyCleanseService,
                                  IndustryCleanseService industryCleanseService,
                                  ConceptCleanseService conceptCleanseService,
                                  MarginCleanseService marginCleanseService,
                                  TradeCalendarService tradeCalendarService,
                                  TradeCalendarMapper tradeCalendarMapper,
                                  IndustryMapper industryMapper,
                                  ConceptMapper conceptMapper) {
        this.aktoolsClient = aktoolsClient;
        this.logMapper = logMapper;
        this.rawDataMapper = rawDataMapper;
        this.stockInfoCleanseService = stockInfoCleanseService;
        this.stockDailyCleanseService = stockDailyCleanseService;
        this.industryCleanseService = industryCleanseService;
        this.conceptCleanseService = conceptCleanseService;
        this.marginCleanseService = marginCleanseService;
        this.tradeCalendarService = tradeCalendarService;
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.industryMapper = industryMapper;
        this.conceptMapper = conceptMapper;
    }

    /**
     * Orchestrate the FETCH → CLEANSE lifecycle for a given data type and trade date.
     *
     * @param dataType  the data type to collect (e.g. STOCK_SPOT, TRADE_CALENDAR)
     * @param tradeDate the trade date to collect for
     * @return execution result message
     */
    public String orchestrate(String dataType, LocalDate tradeDate) {
        String lockKey = dataType + "_" + tradeDate;
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.warn("Collection already in progress for key: {}", lockKey);
            return "已在执行中";
        }

        try {
            log.info("Starting orchestration: dataType={}, tradeDate={}", dataType, tradeDate);

            // Step 1: FETCH
            Result fetchResult = executeFetch(dataType, tradeDate);
            if (!fetchResult.success) {
                log.error("Fetch failed for {} on {}, orchestration stopped", dataType, tradeDate);
                return "采集失败: " + fetchResult.errorMsg;
            }

            // Step 2: CLEANSE
            executeCleanse(dataType, tradeDate, fetchResult.collectionLogId);

            log.info("Orchestration complete: dataType={}, tradeDate={}", dataType, tradeDate);
            return "执行成功";
        } catch (Exception e) {
            log.error("Unexpected error during orchestration: dataType={}, tradeDate={}",
                    dataType, tradeDate, e);
            return "执行异常，请查看系统日志";
        } finally {
            lock.unlock();
            lockMap.remove(lockKey);
        }
    }

    private Result executeFetch(String dataType, LocalDate tradeDate) {
        // Create FETCH log
        DataCollectionLog fetchLog = createLog(dataType, "FETCH", tradeDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        String rawJson = null;
        String errorMsg = null;

        try {
            // Fetch data with retry
            rawJson = fetchWithRetry(dataType, tradeDate, fetchLog.getId());
        } catch (Exception e) {
            errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Fetch failed for {} on {}: {}", dataType, tradeDate, errorMsg);
        }

        if (rawJson == null) {
            // Fetch failed
            fetchLog.setStatus("FAILED");
            fetchLog.setErrorMsg(errorMsg);
            fetchLog.setCompletedAt(LocalDateTime.now());
            logMapper.updateById(fetchLog);
            return new Result(false, null, null, errorMsg);
        }

        // Save raw data
        RawData rawData = new RawData();
        rawData.setCollectionLogId(fetchLog.getId());
        rawData.setDataType(dataType);
        rawData.setTradeDate(tradeDate);
        rawData.setSource("AKTools");
        rawData.setRawJson(rawJson);
        rawData.setFetchAt(LocalDateTime.now());
        rawDataMapper.insert(rawData);

        // Update fetch log to SUCCESS
        fetchLog.setStatus("SUCCESS");
        fetchLog.setRecordCount(estimateRecordCount(rawJson));
        fetchLog.setCompletedAt(LocalDateTime.now());
        logMapper.updateById(fetchLog);

        return new Result(true, rawJson, fetchLog.getId(), null);
    }

    private String fetchWithRetry(String dataType, LocalDate tradeDate, Long collectionLogId) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                log.debug("Fetch attempt {}/{} for {} on {}", attempt + 1, MAX_RETRIES, dataType, tradeDate);
                return dispatchFetch(dataType, tradeDate);
            } catch (Exception e) {
                log.warn("Fetch attempt {}/{} failed for {} on {}: {}",
                        attempt + 1, MAX_RETRIES, dataType, tradeDate, e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw new RuntimeException("All " + MAX_RETRIES + " fetch attempts failed", e);
                }
            }
        }

        throw new RuntimeException("Unreachable: all fetch attempts exhausted");
    }

    private String dispatchFetch(String dataType, LocalDate tradeDate) {
        String dateStr = tradeDate != null ? tradeDate.toString() : "";
        switch (dataType) {
            case "STOCK_INFO":
            case "STOCK_DAILY":
                // Both use stock_zh_a_spot_em — same JSON, different cleanse extraction
                return aktoolsClient.fetchStockSpot();
            case "INDUSTRY_NAME":
                return aktoolsClient.fetchIndustryNames();
            case "INDUSTRY_CONS":
                // Multi-fetch handled in cleanse step; fetch step is a no-op
                return "[]";
            case "CONCEPT_NAME":
                return aktoolsClient.fetchConceptNames();
            case "CONCEPT_CONS":
                // Multi-fetch handled in cleanse step; fetch step is a no-op
                return "[]";
            case "TRADE_CALENDAR":
                return aktoolsClient.fetchTradeCalendar();
            case "MARGIN_DAILY_SSE":
                return aktoolsClient.fetchMarginDetailSse(dateStr);
            case "MARGIN_DAILY_SZSE":
                return aktoolsClient.fetchMarginDetailSzse(dateStr);
            default:
                throw new IllegalArgumentException("Unknown data type: " + dataType);
        }
    }

    private void executeCleanse(String dataType, LocalDate tradeDate, Long collectionLogId) {
        // Create CLEANSE log
        DataCollectionLog cleanseLog = createLog(dataType, "CLEANSE", tradeDate);
        cleanseLog.setStatus("RUNNING");
        cleanseLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(cleanseLog);

        try {
            int recordCount = dispatchCleanse(dataType, tradeDate, collectionLogId);

            cleanseLog.setStatus("SUCCESS");
            cleanseLog.setRecordCount(recordCount);
            cleanseLog.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Cleanse failed for {} on {}: {}", dataType, tradeDate, errorMsg);
            cleanseLog.setStatus("FAILED");
            cleanseLog.setErrorMsg(errorMsg);
            cleanseLog.setCompletedAt(LocalDateTime.now());
        }

        logMapper.updateById(cleanseLog);
    }

    private int dispatchCleanse(String dataType, LocalDate tradeDate, Long collectionLogId) {
        // Fetch the raw JSON from the FETCH step (query RawData by collection_log_id)
        RawData rawData = rawDataMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RawData>()
                        .eq(RawData::getCollectionLogId, collectionLogId)
        );
        String rawJson = rawData != null ? rawData.getRawJson() : null;

        int recordCount;
        switch (dataType) {
            case "STOCK_INFO":
                recordCount = stockInfoCleanseService.cleanse(rawJson, tradeDate);
                break;
            case "STOCK_DAILY":
                recordCount = stockDailyCleanseService.cleanse(rawJson, tradeDate);
                break;
            case "INDUSTRY_NAME":
                recordCount = industryCleanseService.cleanseNames(rawJson);
                break;
            case "INDUSTRY_CONS":
                recordCount = cleanseAllIndustryCons(tradeDate);
                break;
            case "CONCEPT_NAME":
                recordCount = conceptCleanseService.cleanseNames(rawJson);
                break;
            case "CONCEPT_CONS":
                recordCount = cleanseAllConceptCons(tradeDate);
                break;
            case "MARGIN_DAILY_SSE":
                recordCount = marginCleanseService.cleanse(rawJson, "SSE", tradeDate);
                break;
            case "MARGIN_DAILY_SZSE":
                recordCount = marginCleanseService.cleanse(rawJson, "SZSE", tradeDate);
                break;
            case "TRADE_CALENDAR":
                recordCount = tradeCalendarService.syncTradeCalendar();
                break;
            default:
                throw new IllegalArgumentException("Unknown data type: " + dataType);
        }

        log.info("Cleanse dispatch complete: dataType={}, records={}", dataType, recordCount);
        return recordCount;
    }

    /**
     * Query all industry codes from the industry table, fetch constituents for each,
     * and cleanse into stock_industry table.
     */
    private int cleanseAllIndustryCons(LocalDate tradeDate) {
        List<Industry> industries = industryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Industry>()
                        .eq(Industry::getIsDeleted, false)
        );

        if (industries.isEmpty()) {
            log.warn("No industries found in DB to fetch constituents for");
            return 0;
        }

        int total = 0;
        for (Industry industry : industries) {
            try {
                String rawJson = aktoolsClient.fetchIndustryCons(industry.getCode());
                int count = industryCleanseService.cleanseCons(rawJson, industry.getCode(), tradeDate);
                total += count;
            } catch (Exception e) {
                log.error("Failed to cleanse constituents for industry {}", industry.getCode(), e);
            }
        }
        log.info("Cleansed all industry cons: {} total relations across {} industries",
                total, industries.size());
        return total;
    }

    /**
     * Query all concept codes from the concept table, fetch constituents for each,
     * and cleanse into stock_concept table.
     */
    private int cleanseAllConceptCons(LocalDate tradeDate) {
        List<Concept> concepts = conceptMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Concept>()
                        .eq(Concept::getIsDeleted, false)
        );

        if (concepts.isEmpty()) {
            log.warn("No concepts found in DB to fetch constituents for");
            return 0;
        }

        int total = 0;
        for (Concept concept : concepts) {
            try {
                String rawJson = aktoolsClient.fetchConceptCons(concept.getCode());
                int count = conceptCleanseService.cleanseCons(rawJson, concept.getCode(), tradeDate);
                total += count;
            } catch (Exception e) {
                log.error("Failed to cleanse constituents for concept {}", concept.getCode(), e);
            }
        }
        log.info("Cleansed all concept cons: {} total relations across {} concepts",
                total, concepts.size());
        return total;
    }

    private DataCollectionLog createLog(String dataType, String jobType, LocalDate tradeDate) {
        DataCollectionLog entry = new DataCollectionLog();
        entry.setDataType(dataType);
        entry.setJobType(jobType);
        entry.setTradeDate(tradeDate);
        return entry;
    }

    /**
     * Estimate the number of records in the raw JSON.
     */
    private int estimateRecordCount(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < rawJson.length(); i++) {
            if (rawJson.charAt(i) == '{') {
                count++;
            }
        }
        return count;
    }

    /**
     * Backfill margin data for a date range, grouped by ISO week.
     * Skips weeks where all trading dates already have SUCCESS FETCH + CLEANSE logs.
     *
     * @param request backfill parameters (dataType, exchange, date range)
     * @return summary message
     */
    public String backfillMarginByWeek(BackfillRequest request) {
        String dataType = request.getDataType();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        log.info("Starting backfill: dataType={}, start={}, end={}", dataType, startDate, endDate);

        List<TradeCalendar> tradingDays = tradeCalendarMapper.selectTradingDays(startDate, endDate);

        if (tradingDays.isEmpty()) {
            log.info("No trading days found in range: {} to {}", startDate, endDate);
            return "No trading days in range";
        }

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<Integer, List<TradeCalendar>> weeks = new LinkedHashMap<>();

        for (TradeCalendar day : tradingDays) {
            int isoWeek = day.getTradeDate().get(weekFields.weekOfWeekBasedYear());
            int weekYear = day.getTradeDate().get(weekFields.weekBasedYear());
            int weekKey = weekYear * 100 + isoWeek;
            weeks.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(day);
        }

        int totalWeeks = weeks.size();
        int processedDates = 0;
        int skippedWeeks = 0;

        for (Map.Entry<Integer, List<TradeCalendar>> entry : weeks.entrySet()) {
            List<TradeCalendar> weekDays = entry.getValue();
            boolean allComplete = weekDays.stream().allMatch(day -> isDateComplete(dataType, day.getTradeDate()));

            if (allComplete) {
                log.debug("Skipping week {} — all {} dates already complete", entry.getKey(), weekDays.size());
                skippedWeeks++;
            } else {
                for (TradeCalendar day : weekDays) {
                    if (!isDateComplete(dataType, day.getTradeDate())) {
                        orchestrate(dataType, day.getTradeDate());
                        processedDates++;
                    }
                }
            }
        }

        int orchestratedWeeks = totalWeeks - skippedWeeks;
        String result = String.format("Backfill complete: %d dates processed across %d weeks, %d weeks skipped (already complete)",
                processedDates, orchestratedWeeks, skippedWeeks);
        log.info(result);
        return result;
    }

    /**
     * Check if a given date already has SUCCESS logs for both FETCH and CLEANSE.
     */
    private boolean isDateComplete(String dataType, LocalDate tradeDate) {
        DataCollectionLog fetchLog = logMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(dataType, "FETCH", tradeDate);
        DataCollectionLog cleanseLog = logMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(dataType, "CLEANSE", tradeDate);
        return fetchLog != null && "SUCCESS".equals(fetchLog.getStatus())
                && cleanseLog != null && "SUCCESS".equals(cleanseLog.getStatus());
    }

    private static class Result {
        final boolean success;
        final String rawJson;
        final Long collectionLogId;
        final String errorMsg;

        Result(boolean success, String rawJson, Long collectionLogId, String errorMsg) {
            this.success = success;
            this.rawJson = rawJson;
            this.collectionLogId = collectionLogId;
            this.errorMsg = errorMsg;
        }
    }
}
