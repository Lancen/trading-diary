package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.StockInfoMapper;
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
    private final StockInfoMapper stockInfoMapper;

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
                                  ConceptMapper conceptMapper,
                                  StockInfoMapper stockInfoMapper) {
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
        this.stockInfoMapper = stockInfoMapper;
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

            // STOCK_DAILY 复用 STOCK_INFO 的 FETCH 数据（同一 API，同一份 JSON）
            if ("STOCK_DAILY".equals(dataType)) {
                Long stockInfoLogId = findExistingFetchLog("STOCK_INFO", tradeDate);
                if (stockInfoLogId == null) {
                    return "请先采集股票基础信息（STOCK_INFO），日线行情复用同一份数据";
                }
                log.info("STOCK_DAILY 复用 STOCK_INFO FETCH 数据: tradeDate={}, logId={}", tradeDate, stockInfoLogId);
                executeCleanse(dataType, tradeDate, stockInfoLogId);
                return "执行成功（复用股票基础信息采集数据）";
            }

            // 检查是否已有成功的 FETCH，有则复用 raw_data 跳过重复采集
            Long reuseLogId = findExistingFetchLog(dataType, tradeDate);
            if (reuseLogId != null) {
                log.info("复用已有 FETCH 数据: dataType={}, tradeDate={}, logId={}",
                        dataType, tradeDate, reuseLogId);
                executeCleanse(dataType, tradeDate, reuseLogId);
                if ("STOCK_INFO".equals(dataType)) {
                    executeCleanse("STOCK_DAILY", tradeDate, reuseLogId);
                }
                return "执行成功（复用已有采集数据）";
            }

            // Step 1: FETCH
            Result fetchResult = executeFetch(dataType, tradeDate);
            if (!fetchResult.success) {
                return "采集失败: " + fetchResult.errorMsg;
            }

            // Step 2: CLEANSE
            executeCleanse(dataType, tradeDate, fetchResult.collectionLogId);

            // STOCK_INFO 完成后联动执行 STOCK_DAILY CLEANSE（复用同一份 FETCH 数据）
            if ("STOCK_INFO".equals(dataType)) {
                log.info("STOCK_INFO done, 联动执行 STOCK_DAILY CLEANSE: tradeDate={}", tradeDate);
                executeCleanse("STOCK_DAILY", tradeDate, fetchResult.collectionLogId);
            }

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
                        throw new RuntimeException("重试被中断", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw new RuntimeException("共 " + MAX_RETRIES + " 次采集尝试全部失败", e);
                }
            }
        }

        throw new RuntimeException("所有采集尝试已用尽");
    }

    private static final java.time.format.DateTimeFormatter YMD = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");

    private String dispatchFetch(String dataType, LocalDate tradeDate) {
        String dateStr = tradeDate != null ? tradeDate.format(YMD) : "";
        switch (dataType) {
            case "STOCK_INFO":
                return aktoolsClient.fetchStockSpot();
            case "STOCK_DAILY":
                // STOCK_DAILY 复用 STOCK_INFO 的 FETCH 数据，不应走到这里
                throw new IllegalStateException("STOCK_DAILY 应复用 STOCK_INFO FETCH，不应直接调用");
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
                throw new IllegalArgumentException("未知数据类型: " + dataType);
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
                throw new IllegalArgumentException("未知数据类型: " + dataType);
        }

        log.info("Cleanse dispatch complete: dataType={}, records={}", dataType, recordCount);
        return recordCount;
    }

    /**
     * Cleanse industry constituents into stock_industry table.
     *
     * 成分股数据已改为通过 Playwright 从同花顺抓取，不再通过 AKTools HTTP API 获取。
     * 请先运行 scripts/scrape_ths_constituents.py 生成 JSON，
     * 然后通过管理后台或 SQL 导入到 stock_industry 表。
     *
     * 此方法现在依赖 DB 中已有的 stock_industry 数据做变化检测。
     */
    private int cleanseAllIndustryCons(LocalDate tradeDate) {
        List<Industry> industries = industryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Industry>()
                        .eq(Industry::getIsDeleted, false)
        );

        if (industries.isEmpty()) {
            log.warn("No industries found in DB — run INDUSTRY_NAME and Playwright import first");
            return 0;
        }

        log.info("行业成分股变化检测: {} 个行业（成分股数据需通过 Playwright 预先导入）", industries.size());
        // 成分股通过 Playwright 抓取后直接写入 stock_industry 表
        // 此方法仅做变化检测和日志记录，实际写表在 Playwright 导入阶段完成
        return 0;
    }

    /**
     * Cleanse concept constituents into stock_concept table.
     *
     * 成分股数据已改为通过 Playwright 从同花顺抓取，不再通过 AKTools HTTP API 获取。
     * 请先运行 scripts/scrape_ths_constituents.py 生成 JSON，
     * 然后通过管理后台或 SQL 导入到 stock_concept 表。
     */
    private int cleanseAllConceptCons(LocalDate tradeDate) {
        List<Concept> concepts = conceptMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Concept>()
                        .eq(Concept::getIsDeleted, false)
        );

        if (concepts.isEmpty()) {
            log.warn("No concepts found in DB — run CONCEPT_NAME and Playwright import first");
            return 0;
        }

        log.info("概念成分股变化检测: {} 个概念（成分股数据需通过 Playwright 预先导入）", concepts.size());
        // 成分股通过 Playwright 抓取后直接写入 stock_concept 表
        return 0;
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
            return "此范围无交易日";
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
        String result = String.format("补采完成: %d 个日期已处理（共 %d 周），%d 周已跳过（数据完整）",
                processedDates, orchestratedWeeks, skippedWeeks);
        log.info(result);
        return result;
    }

    /**
     * Backfill historical stock daily data for all stocks in a date range.
     * Iterates through each stock, calls stock_zh_a_hist API, and cleanses into stock_daily.
     *
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return summary message
     */
    public String backfillStockDaily(LocalDate startDate, LocalDate endDate) {
        String start = startDate.toString();
        String end = endDate.toString();
        log.info("Starting stock daily backfill: {} to {}", start, end);

        List<StockInfo> stocks = stockInfoMapper.selectList(null);
        if (stocks.isEmpty()) {
            return "stock_info 表无数据 — 请先采集 STOCK_INFO";
        }

        int success = 0;
        int failed = 0;
        for (StockInfo stock : stocks) {
            String code = stock.getCode();
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                String rawJson = aktoolsClient.fetchStockDaily(code, start, end);
                int records = stockDailyCleanseService.cleanseHistJson(rawJson, code);
                log.debug("Stock {} backfilled: {} records", code, records);
                success++;
            } catch (Exception e) {
                log.error("Failed to backfill stock {}", code, e);
                failed++;
            }
            aktoolsClient.sleepBetweenCalls();
        }

        String result = String.format("股票日线补采完成: 成功 %d 只，失败 %d 只（共 %d 只）",
                success, failed, stocks.size());
        log.info(result);
        return result;
    }

    /**
     * 查找当天已有的成功 FETCH 日志，返回 raw_data 的 collection_log_id。
     * 若不存在则返回 null，需执行完整 FETCH。
     */
    private Long findExistingFetchLog(String dataType, LocalDate tradeDate) {
        DataCollectionLog fetchLog = logMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                dataType, "FETCH", tradeDate);
        if (fetchLog != null && "SUCCESS".equals(fetchLog.getStatus())) {
            return fetchLog.getId();
        }
        return null;
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
