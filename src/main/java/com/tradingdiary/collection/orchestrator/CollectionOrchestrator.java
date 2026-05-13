package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.RawDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public CollectionOrchestrator(AKToolsClient aktoolsClient,
                                  DataCollectionLogMapper logMapper,
                                  RawDataMapper rawDataMapper) {
        this.aktoolsClient = aktoolsClient;
        this.logMapper = logMapper;
        this.rawDataMapper = rawDataMapper;
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
            return "执行异常: " + e.getMessage();
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
            case "STOCK_SPOT":
                return aktoolsClient.fetchStockSpot();
            case "STOCK_DAILY":
                return aktoolsClient.fetchStockDaily("sh000001", dateStr, dateStr);
            case "INDUSTRY_NAMES":
                return aktoolsClient.fetchIndustryNames();
            case "INDUSTRY_CONS":
                return aktoolsClient.fetchIndustryCons("sh000001");
            case "CONCEPT_NAMES":
                return aktoolsClient.fetchConceptNames();
            case "CONCEPT_CONS":
                return aktoolsClient.fetchConceptCons("sh000001");
            case "TRADE_CALENDAR":
                return aktoolsClient.fetchTradeCalendar();
            case "MARGIN_DETAIL_SSE":
                return aktoolsClient.fetchMarginDetailSse(dateStr);
            case "MARGIN_DETAIL_SZSE":
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
            // Placeholder: dispatch to cleanse service (Phase 3)
            dispatchCleanse(dataType, tradeDate, collectionLogId);

            cleanseLog.setStatus("SUCCESS");
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

    private void dispatchCleanse(String dataType, LocalDate tradeDate, Long collectionLogId) {
        switch (dataType) {
            case "TRADE_CALENDAR":
                log.info("Cleanse placeholder: TRADE_CALENDAR for {}", tradeDate);
                break;
            case "STOCK_SPOT":
            case "STOCK_DAILY":
            case "INDUSTRY_NAMES":
            case "INDUSTRY_CONS":
            case "CONCEPT_NAMES":
            case "CONCEPT_CONS":
            case "MARGIN_DETAIL_SSE":
            case "MARGIN_DETAIL_SZSE":
                log.info("Cleanse placeholder: {} for {}", dataType, tradeDate);
                break;
            default:
                throw new IllegalArgumentException("Unknown data type: " + dataType);
        }
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
