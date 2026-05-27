package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.handler.DataTypeHandler;
import com.tradingdiary.collection.handler.SectorIndexHandler;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 数据采集编排器，通过 DataTypeHandler registry 协调 FETCH 和 CLEANSE 两阶段流程
 */
@Service
public class CollectionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CollectionOrchestrator.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    private final Map<String, DataTypeHandler> handlerMap;
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private final DataCollectionLogMapper logMapper;
    private final RawDataMapper rawDataMapper;
    private final TradeCalendarMapper tradeCalendarMapper;

    public CollectionOrchestrator(List<DataTypeHandler> handlers,
                                  DataCollectionLogMapper logMapper,
                                  RawDataMapper rawDataMapper,
                                  TradeCalendarMapper tradeCalendarMapper) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(DataTypeHandler::dataType, h -> h));
        this.logMapper = logMapper;
        this.rawDataMapper = rawDataMapper;
        this.tradeCalendarMapper = tradeCalendarMapper;
    }

    @Async("collectionExecutor")
    public CompletableFuture<String> orchestrateAsync(String dataType, LocalDate tradeDate) {
        String result = orchestrate(dataType, tradeDate);
        return CompletableFuture.completedFuture(result);
    }

    @Async("collectionExecutor")
    public CompletableFuture<String> orchestrateDailyAsync(List<String> dataTypes, LocalDate tradeDate) {
        int skipped = 0;
        int success = 0;
        int failed = 0;
        List<String> failedTypes = new ArrayList<>();
        for (String dataType : dataTypes) {
            if (isDateComplete(dataType, tradeDate)) {
                skipped++;
                log.info("一键采集: 跳过 {} ({}) — 已完成", dataType, tradeDate);
                continue;
            }
            try {
                log.info("一键采集: 开始 {} ({})", dataType, tradeDate);
                String result = orchestrate(dataType, tradeDate);
                if (result.contains("成功") || result.contains("复用")) {
                    success++;
                    log.info("一键采集: 完成 {} → {}", dataType, result);
                } else {
                    failed++;
                    failedTypes.add(dataType);
                    log.warn("一键采集: 失败 {} → {}", dataType, result);
                }
            } catch (Exception e) {
                failed++;
                failedTypes.add(dataType);
                log.error("一键采集: 异常 {} → {}", dataType, e.getMessage());
            }
        }
        String summary = String.format("一键采集完成: %d 成功, %d 跳过(已完成), %d 失败%s",
                success, skipped, failed, failedTypes.isEmpty() ? "" : " (失败: " + String.join(", ", failedTypes) + ")");
        log.info(summary);
        return CompletableFuture.completedFuture(summary);
    }

    /**
     * 编排执行数据采集和清洗流程
     *
     * @param dataType 数据类型（如 STOCK_SPOT、MARGIN_DAILY_SSE 等）
     * @param tradeDate 交易日期
     * @return 执行结果描述
     */
    public String orchestrate(String dataType, LocalDate tradeDate) {
        DataTypeHandler handler = handlerMap.get(dataType);
        if (handler == null) {
            return "未知数据类型: " + dataType;
        }

        String lockKey = dataType + "_" + tradeDate;
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.warn("Collection already in progress for key: {}", lockKey);
            return "已在执行中";
        }

        try {
            log.info("Starting orchestration: dataType={}, tradeDate={}", dataType, tradeDate);

            // 板块指数K线：使用 SectorIndexHandler 的专用路径
            if (handler instanceof SectorIndexHandler sectorHandler) {
                Long reuseLogId = findExistingFetchLog(dataType, tradeDate);
                if (reuseLogId != null) {
                    log.info("复用已有 FETCH 数据: dataType={}, tradeDate={}, logId={}", dataType, tradeDate, reuseLogId);
                    executeCleanse(handler, dataType, tradeDate, reuseLogId);
                    return "执行成功（复用已有采集数据）";
                }
                Long fetchLogId = executeSectorIndexFetch(sectorHandler, dataType, tradeDate, tradeDate);
                if (fetchLogId == null) {
                    return "采集失败: 无板块数据";
                }
                executeCleanse(handler, dataType, tradeDate, fetchLogId);
                return "执行成功";
            }

            // 检查是否已有成功的 FETCH，有则复用
            Long reuseLogId = findExistingFetchLog(dataType, tradeDate);
            if (reuseLogId != null) {
                log.info("复用已有 FETCH 数据: dataType={}, tradeDate={}, logId={}", dataType, tradeDate, reuseLogId);
                executeCleanse(handler, dataType, tradeDate, reuseLogId);
                return "执行成功（复用已有采集数据）";
            }

            // 第一步：FETCH
            Result fetchResult = executeFetch(handler, dataType, tradeDate);
            if (!fetchResult.success) {
                return "采集失败: " + fetchResult.errorMsg;
            }

            // 第二步：CLEANSE
            executeCleanse(handler, dataType, tradeDate, fetchResult.collectionLogId);

            log.info("Orchestration complete: dataType={}, tradeDate={}", dataType, tradeDate);
            return "执行成功";
        } catch (Exception e) {
            log.error("Unexpected error during orchestration: dataType={}, tradeDate={}", dataType, tradeDate, e);
            return "执行异常，请查看系统日志";
        } finally {
            lock.unlock();
            lockMap.remove(lockKey);
        }
    }

    private Result executeFetch(DataTypeHandler handler, String dataType, LocalDate tradeDate) {
        DataCollectionLog fetchLog = createLog(dataType, "FETCH", tradeDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        String rawJson = null;
        String errorMsg = null;

        try {
            rawJson = fetchWithRetry(handler, dataType, tradeDate);
        } catch (Exception e) {
            errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Fetch failed for {} on {}: {}", dataType, tradeDate, errorMsg);
        }

        if (rawJson == null) {
            fetchLog.setStatus("FAILED");
            fetchLog.setErrorMsg(errorMsg);
            fetchLog.setCompletedAt(LocalDateTime.now());
            logMapper.updateById(fetchLog);
            return new Result(false, null, null, errorMsg);
        }

        // 保存原始数据
        RawData rawData = new RawData();
        rawData.setCollectionLogId(fetchLog.getId());
        rawData.setDataType(dataType);
        rawData.setTradeDate(tradeDate);
        rawData.setSource("AKTools");
        rawData.setRawJson(rawJson);
        rawData.setFetchAt(LocalDateTime.now());
        rawDataMapper.insert(rawData);

        fetchLog.setStatus("SUCCESS");
        fetchLog.setRecordCount(estimateRecordCount(rawJson));
        fetchLog.setCompletedAt(LocalDateTime.now());
        logMapper.updateById(fetchLog);

        return new Result(true, rawJson, fetchLog.getId(), null);
    }

    private String fetchWithRetry(DataTypeHandler handler, String dataType, LocalDate tradeDate) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                log.debug("Fetch attempt {}/{} for {} on {}", attempt + 1, MAX_RETRIES, dataType, tradeDate);
                return handler.fetch(tradeDate);
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
                    String rootCause = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    throw new RuntimeException("共 " + MAX_RETRIES + " 次采集尝试全部失败，错误: " + rootCause, e);
                }
            }
        }

        throw new RuntimeException("所有采集尝试已用尽");
    }

    private void executeCleanse(DataTypeHandler handler, String dataType, LocalDate tradeDate, Long collectionLogId) {
        DataCollectionLog cleanseLog = createLog(dataType, "CLEANSE", tradeDate);
        cleanseLog.setStatus("RUNNING");
        cleanseLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(cleanseLog);

        try {
            // 从 FETCH 阶段获取原始 JSON
            RawData rawData = rawDataMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RawData>()
                            .eq(RawData::getCollectionLogId, collectionLogId));
            String rawJson = rawData != null ? rawData.getRawJson() : null;

            int recordCount = handler.cleanse(rawJson, tradeDate);

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

    private Long executeSectorIndexFetch(SectorIndexHandler sectorHandler, String dataType,
                                           LocalDate startDate, LocalDate endDate) {
        DataCollectionLog fetchLog = createLog(dataType, "FETCH", endDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        int successCount;
        try {
            successCount = sectorHandler.fetchSectors(startDate, endDate, fetchLog.getId());
        } catch (Exception e) {
            fetchLog.setStatus("FAILED");
            fetchLog.setErrorMsg(e.getMessage());
            fetchLog.setCompletedAt(LocalDateTime.now());
            logMapper.updateById(fetchLog);
            return null;
        }

        fetchLog.setStatus("SUCCESS");
        fetchLog.setRecordCount(successCount);
        fetchLog.setCompletedAt(LocalDateTime.now());
        logMapper.updateById(fetchLog);
        return fetchLog.getId();
    }

    private DataCollectionLog createLog(String dataType, String jobType, LocalDate tradeDate) {
        DataCollectionLog entry = new DataCollectionLog();
        entry.setDataType(dataType);
        entry.setJobType(jobType);
        entry.setTradeDate(tradeDate);
        return entry;
    }

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

    // ===== 回补方法 =====

    @Async("collectionExecutor")
    public CompletableFuture<String> backfillMarginByWeekAsync(BackfillRequest request) {
        String result = backfillMarginByWeek(request);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 按周补采两融数据
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

    @Async("collectionExecutor")
    public CompletableFuture<String> backfillStockDailyAsync(LocalDate startDate, LocalDate endDate) {
        String result = backfillStockDaily(startDate, endDate);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 补采股票日线历史数据（Tushare 全市场一次拉取）
     */
    public String backfillStockDaily(LocalDate startDate, LocalDate endDate) {
        DataTypeHandler tushareHandler = handlerMap.get("STOCK_DAILY_TUSHARE");
        if (tushareHandler == null) {
            return "STOCK_DAILY_TUSHARE handler 未注册";
        }

        log.info("Starting stock daily backfill (Tushare): {} to {}", startDate, endDate);

        int totalRecords = 0;
        int failedDays = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            try {
                String rawJson = tushareHandler.fetch(date);
                int count = tushareHandler.cleanse(rawJson, date);
                totalRecords += count;
                log.info("Tushare daily backfill: {} → {} records", date, count);
            } catch (Exception e) {
                log.error("Tushare daily backfill failed for {}", date, e);
                failedDays++;
            }
            date = date.plusDays(1);
        }

        String result = String.format("日线补采完成: %d 条记录，%d 天失败", totalRecords, failedDays);
        log.info(result);
        return result;
    }

    @Async("collectionExecutor")
    public CompletableFuture<String> backfillSectorIndexDailyAsync(String sectorType, LocalDate startDate, LocalDate endDate) {
        String result = backfillSectorIndexDaily(sectorType, startDate, endDate);
        return CompletableFuture.completedFuture(result);
    }

    public String backfillSectorIndexDaily(String sectorType, LocalDate startDate, LocalDate endDate) {
        String dataType = "INDUSTRY".equals(sectorType) ? "INDUSTRY_INDEX_DAILY" : "CONCEPT_INDEX_DAILY";
        DataTypeHandler handler = handlerMap.get(dataType);
        if (handler == null || !(handler instanceof SectorIndexHandler sectorHandler)) {
            return dataType + " handler 未注册";
        }

        log.info("Starting sector index daily backfill: sectorType={}, {} to {}", sectorType, startDate, endDate);

        Long fetchLogId = executeSectorIndexFetch(sectorHandler, dataType, startDate, endDate);
        if (fetchLogId == null) {
            return "板块指数K线补采失败: 无板块数据";
        }

        int cleanseCount = 0;
        List<RawData> rawDatas = rawDataMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RawData>()
                        .eq(RawData::getCollectionLogId, fetchLogId));
        for (RawData rd : rawDatas) {
            try {
                String rawJson = rd.getRawJson();
                if (rawJson != null && !rawJson.equals("[]")) {
                    cleanseCount += handler.cleanse(rawJson, endDate);
                }
            } catch (Exception e) {
                log.error("Failed to cleanse sector index daily for {}: {}", rd.getSectorCode(), e.getMessage());
            }
        }

        String result = String.format("板块指数K线补采完成: %d 个板块，%d 条记录", rawDatas.size(), cleanseCount);
        log.info(result);
        return result;
    }

    // ===== 公共查询方法 =====

    private Long findExistingFetchLog(String dataType, LocalDate tradeDate) {
        DataCollectionLog fetchLog = logMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                dataType, "FETCH", tradeDate);
        if (fetchLog != null && "SUCCESS".equals(fetchLog.getStatus())) {
            return fetchLog.getId();
        }
        return null;
    }

    /**
     * 检查给定日期是否已有成功的 FETCH 和 CLEANSE 日志
     */
    public boolean isDateComplete(String dataType, LocalDate tradeDate) {
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