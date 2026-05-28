package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.handler.DataTypeHandler;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.RetryPolicy;
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

    private final RetryPolicy retryPolicy = RetryPolicy.DEFAULT;

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

            // 检查是否已有成功的 FETCH，有则复用
            Long reuseLogId = findExistingFetchLog(dataType, tradeDate);
            if (reuseLogId != null) {
                log.info("复用已有 FETCH 数据: dataType={}, tradeDate={}, logId={}", dataType, tradeDate, reuseLogId);
                executeCleanse(handler, dataType, tradeDate, reuseLogId);
                return "执行成功（复用已有采集数据）";
            }

            // 第一步：FETCH
            Long fetchLogId = executeFetch(handler, dataType, tradeDate);
            if (fetchLogId == null) {
                return "采集失败";
            }

            // 第二步：CLEANSE
            executeCleanse(handler, dataType, tradeDate, fetchLogId);

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

    private Long executeFetch(DataTypeHandler handler, String dataType, LocalDate tradeDate) {
        DataCollectionLog fetchLog = createLog(dataType, "FETCH", tradeDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        FetchResult result = null;
        String errorMsg = null;

        try {
            result = fetchWithRetry(handler, dataType, tradeDate);
        } catch (Exception e) {
            errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Fetch failed for {} on {}: {}", dataType, tradeDate, errorMsg);
        }

        if (result == null || !result.isSuccess()) {
            fetchLog.setStatus("FAILED");
            fetchLog.setErrorMsg(errorMsg != null ? errorMsg : "采集结果为空");
            fetchLog.setCompletedAt(LocalDateTime.now());
            logMapper.updateById(fetchLog);
            return null;
        }

        if (result.getType() == FetchResult.Type.MULTI_SECTOR) {
            // MULTI_SECTOR：handler 内部已保存多条 raw_data 并更新了自己的日志
            // 关联当前编排器创建的日志，标记成功
            fetchLog.setStatus("SUCCESS");
            fetchLog.setRecordCount(result.getSectorCount());
            fetchLog.setCompletedAt(LocalDateTime.now());
            logMapper.updateById(fetchLog);
            return result.getCollectionLogId();
        }

        // SINGLE：保存单条 raw_data
        String rawJson = result.getRawJson();
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

        return fetchLog.getId();
    }

    private FetchResult fetchWithRetry(DataTypeHandler handler, String dataType, LocalDate tradeDate) {
        for (int attempt = 0; attempt < retryPolicy.getMaxRetries(); attempt++) {
            try {
                log.debug("Fetch attempt {}/{} for {} on {}", attempt + 1, retryPolicy.getMaxRetries(), dataType, tradeDate);
                return handler.fetch(tradeDate);
            } catch (Exception e) {
                log.warn("Fetch attempt {}/{} failed for {} on {}: {}",
                        attempt + 1, retryPolicy.getMaxRetries(), dataType, tradeDate, e.getMessage());

                if (attempt < retryPolicy.getMaxRetries() - 1) {
                    try {
                        Thread.sleep(retryPolicy.getBackoffMs(attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    String rootCause = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    throw new RuntimeException("共 " + retryPolicy.getMaxRetries() + " 次采集尝试全部失败，错误: " + rootCause, e);
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

        int processedDates = 0;
        int skippedDates = 0;
        int failedDates = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            if (isDateComplete("STOCK_DAILY_TUSHARE", date)) {
                skippedDates++;
                log.debug("Tushare daily backfill: {} already complete, skipping", date);
            } else {
                String result = orchestrate("STOCK_DAILY_TUSHARE", date);
                if (result.contains("成功")) {
                    processedDates++;
                    log.info("Tushare daily backfill: {} -> {}", date, result);
                } else {
                    failedDates++;
                    log.error("Tushare daily backfill failed for {}: {}", date, result);
                }
            }
            date = date.plusDays(1);
        }

        String summary = String.format("日线补采完成: %d 个日期已处理，%d 个日期已跳过，%d 个日期失败",
                processedDates, skippedDates, failedDates);
        log.info(summary);
        return summary;
    }

    @Async("collectionExecutor")
    public CompletableFuture<String> backfillSectorIndexDailyAsync(String sectorType, LocalDate startDate, LocalDate endDate) {
        String result = backfillSectorIndexDaily(sectorType, startDate, endDate);
        return CompletableFuture.completedFuture(result);
    }

    public String backfillSectorIndexDaily(String sectorType, LocalDate startDate, LocalDate endDate) {
        String dataType = "INDUSTRY".equals(sectorType) ? "INDUSTRY_INDEX_DAILY" : "CONCEPT_INDEX_DAILY";
        DataTypeHandler handler = handlerMap.get(dataType);
        if (handler == null) {
            return dataType + " handler 未注册";
        }

        log.info("Starting sector index daily backfill: sectorType={}, {} to {}", sectorType, startDate, endDate);

        int processedDates = 0;
        int failedDates = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            if (isDateComplete(dataType, date)) {
                log.debug("Sector index daily backfill: {} already complete, skipping", date);
            } else {
                Long collectionLogId = executeFetch(handler, dataType, date);
                if (collectionLogId != null) {
                    executeCleanse(handler, dataType, date, collectionLogId);
                    processedDates++;
                    log.info("Sector index daily backfill: {} -> {} sectors", date, handler.dataType());
                } else {
                    failedDates++;
                    log.error("Sector index daily backfill failed for {}", date);
                }
            }
            date = date.plusDays(1);
        }

        String summary = String.format("板块指数K线补采完成: %d 个日期已处理，%d 个日期失败",
                processedDates, failedDates);
        log.info(summary);
        return summary;
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
}