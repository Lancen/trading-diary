package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.CollectionConstants;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.client.TushareClient;
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
import com.tradingdiary.service.collection.MarginMacroCleanseService;
import com.tradingdiary.service.collection.MarketIndexDailyCleanseService;
import com.tradingdiary.service.collection.SectorIndexDailyCleanseService;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import com.tradingdiary.service.collection.StockInfoCleanseService;
import com.tradingdiary.service.collection.TradeCalendarService;
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

/**
 * 数据采集编排器，协调采集（FETCH）和清洗（CLEANSE）两阶段的数据处理流程
 */
@Service
public class CollectionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CollectionOrchestrator.class);

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final AKToolsClient aktoolsClient;
    private final TushareClient tushareClient;
    private final DataCollectionLogMapper logMapper;
    private final RawDataMapper rawDataMapper;
    private final StockInfoCleanseService stockInfoCleanseService;
    private final StockDailyCleanseService stockDailyCleanseService;
    private final IndustryCleanseService industryCleanseService;
    private final ConceptCleanseService conceptCleanseService;
    private final MarginCleanseService marginCleanseService;
    private final MarginMacroCleanseService marginMacroCleanseService;
    private final MarketIndexDailyCleanseService marketIndexDailyCleanseService;
    private final SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private final TradeCalendarService tradeCalendarService;
    private final TradeCalendarMapper tradeCalendarMapper;
    private final IndustryMapper industryMapper;
    private final ConceptMapper conceptMapper;
    private final StockInfoMapper stockInfoMapper;

    public CollectionOrchestrator(AKToolsClient aktoolsClient,
                                  TushareClient tushareClient,
                                  DataCollectionLogMapper logMapper,
                                  RawDataMapper rawDataMapper,
                                  StockInfoCleanseService stockInfoCleanseService,
                                  StockDailyCleanseService stockDailyCleanseService,
                                  IndustryCleanseService industryCleanseService,
                                  ConceptCleanseService conceptCleanseService,
                                  MarginCleanseService marginCleanseService,
                                  MarginMacroCleanseService marginMacroCleanseService,
                                  MarketIndexDailyCleanseService marketIndexDailyCleanseService,
                                  SectorIndexDailyCleanseService sectorIndexDailyCleanseService,
                                  TradeCalendarService tradeCalendarService,
                                  TradeCalendarMapper tradeCalendarMapper,
                                  IndustryMapper industryMapper,
                                  ConceptMapper conceptMapper,
                                  StockInfoMapper stockInfoMapper) {
        this.aktoolsClient = aktoolsClient;
        this.tushareClient = tushareClient;
        this.logMapper = logMapper;
        this.rawDataMapper = rawDataMapper;
        this.stockInfoCleanseService = stockInfoCleanseService;
        this.stockDailyCleanseService = stockDailyCleanseService;
        this.industryCleanseService = industryCleanseService;
        this.conceptCleanseService = conceptCleanseService;
        this.marginCleanseService = marginCleanseService;
        this.marginMacroCleanseService = marginMacroCleanseService;
        this.marketIndexDailyCleanseService = marketIndexDailyCleanseService;
        this.sectorIndexDailyCleanseService = sectorIndexDailyCleanseService;
        this.tradeCalendarService = tradeCalendarService;
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.industryMapper = industryMapper;
        this.conceptMapper = conceptMapper;
        this.stockInfoMapper = stockInfoMapper;
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
     * <p>
     * 根据数据类型和交易日，执行完整的数据采集（FETCH）和清洗（CLEANSE）流程。
     * 支持数据复用机制，避免重复采集。使用分布式锁确保同一数据类型的采集任务不会并发执行。
     * </p>
     *
     * @param dataType 数据类型，如"STOCK_INFO"、"STOCK_DAILY"、"MARGIN_DAILY_SSE"等
     * @param tradeDate 交易日期
     * @return 执行结果描述，包含成功或失败信息
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

            // 板块指数K线：FETCH阶段遍历所有板块逐个调API，每板块一条raw_data
            if ("INDUSTRY_INDEX_DAILY".equals(dataType) || "CONCEPT_INDEX_DAILY".equals(dataType)) {
                String sectorType = "INDUSTRY_INDEX_DAILY".equals(dataType) ? "INDUSTRY" : "CONCEPT";
                Long reuseLogId = findExistingFetchLog(dataType, tradeDate);
                if (reuseLogId != null) {
                    log.info("复用已有 FETCH 数据: dataType={}, tradeDate={}, logId={}", dataType, tradeDate, reuseLogId);
                    executeCleanse(dataType, tradeDate, reuseLogId);
                    return "执行成功（复用已有采集数据）";
                }
                Long fetchLogId = executeSectorIndexFetch(dataType, sectorType, tradeDate, tradeDate);
                if (fetchLogId == null) {
                    return "采集失败: 无板块数据";
                }
                executeCleanse(dataType, tradeDate, fetchLogId);
                return "执行成功";
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

            // 第一步：FETCH（采集）
            Result fetchResult = executeFetch(dataType, tradeDate);
            if (!fetchResult.success) {
                return "采集失败: " + fetchResult.errorMsg;
            }

            // 第二步：CLEANSE（清洗）
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
        // 创建 FETCH 日志
        DataCollectionLog fetchLog = createLog(dataType, "FETCH", tradeDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        String rawJson = null;
        String errorMsg = null;

        try {
            // 带重试机制的数据采集
            rawJson = fetchWithRetry(dataType, tradeDate, fetchLog.getId());
        } catch (Exception e) {
            errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Fetch failed for {} on {}: {}", dataType, tradeDate, errorMsg);
        }

        if (rawJson == null) {
            // 采集失败
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

        // 更新采集日志状态为成功
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
                    String rootCause = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    throw new RuntimeException("共 " + MAX_RETRIES + " 次采集尝试全部失败，错误: " + rootCause, e);
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
                // 多次采集在清洗阶段处理；采集阶段为空操作
                return "[]";
            case "CONCEPT_NAME":
                return aktoolsClient.fetchConceptNames();
            case "CONCEPT_CONS":
                // 多次采集在清洗阶段处理；采集阶段为空操作
                return "[]";
            case "TRADE_CALENDAR":
                return aktoolsClient.fetchTradeCalendar();
            case "MARGIN_DAILY_SSE":
                return aktoolsClient.fetchMarginDetailSse(dateStr);
            case "MARGIN_DAILY_SZSE":
                return aktoolsClient.fetchMarginDetailSzse(dateStr);
            case "MARGIN_MACRO_SSE":
                return aktoolsClient.fetchMacroMarginSh();
            case "MARGIN_MACRO_SZSE":
                return aktoolsClient.fetchMacroMarginSz();
            case "MARKET_INDEX_DAILY":
                return fetchAllMarketIndices(tradeDate);
            case "INDUSTRY_INDEX_DAILY":
            case "CONCEPT_INDEX_DAILY":
                throw new IllegalStateException(dataType + " 应通过 executeSectorIndexFetch 处理，不应走到 dispatchFetch");
            default:
                throw new IllegalArgumentException("未知数据类型: " + dataType);
        }
    }

    private void executeCleanse(String dataType, LocalDate tradeDate, Long collectionLogId) {
        // 创建 CLEANSE 日志
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
        // 从 FETCH 阶段获取原始 JSON（通过 collection_log_id 查询 RawData）
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
            case "MARGIN_MACRO_SSE":
                recordCount = marginMacroCleanseService.cleanse(rawJson, "SSE");
                break;
            case "MARGIN_MACRO_SZSE":
                recordCount = marginMacroCleanseService.cleanse(rawJson, "SZSE");
                break;
            case "MARKET_INDEX_DAILY":
                recordCount = cleanseAllMarketIndices(tradeDate);
                break;
            case "INDUSTRY_INDEX_DAILY":
                recordCount = cleanseAllSectorIndices("INDUSTRY", tradeDate);
                break;
            case "CONCEPT_INDEX_DAILY":
                recordCount = cleanseAllSectorIndices("CONCEPT", tradeDate);
                break;
            default:
                throw new IllegalArgumentException("未知数据类型: " + dataType);
        }

        log.info("Cleanse dispatch complete: dataType={}, records={}", dataType, recordCount);
        return recordCount;
    }

    /**
     * 清洗行业成分股数据写入 stock_industry 表。
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
     * 清洗概念成分股数据写入 stock_concept 表。
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

    private static final List<String> MARKET_INDEX_CODES = List.of(
            "sh000001", "sz399001", "sz399006", "sh000300", "sh000905",
            "sh000016", "sh000688", "sh000852"
    );

    private String fetchAllMarketIndices(LocalDate tradeDate) {
        String dateStr = tradeDate != null ? tradeDate.format(YMD) : "";
        StringBuilder combined = new StringBuilder("[");
        for (int i = 0; i < MARKET_INDEX_CODES.size(); i++) {
            String code = MARKET_INDEX_CODES.get(i);
            try {
                String json = aktoolsClient.fetchMarketIndexDaily(code, dateStr, dateStr);
                if (json != null && !json.equals("[]")) {
                    if (combined.length() > 1) combined.append(",");
                    combined.append("\"").append(code).append("\":").append(json);
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to fetch market index daily for {}: {}", code, e.getMessage());
            }
        }
        combined.append("]");
        return combined.toString();
    }

    private int cleanseAllMarketIndices(LocalDate tradeDate) {
        int totalRecords = 0;
        for (String indexCode : MARKET_INDEX_CODES) {
            try {
                String startDate = tradeDate != null ? tradeDate.format(YMD) : "";
                String endDate = startDate;
                String rawJson = aktoolsClient.fetchMarketIndexDaily(indexCode, startDate, endDate);
                if (rawJson != null && !rawJson.equals("[]")) {
                    int count = marketIndexDailyCleanseService.cleanse(rawJson, indexCode);
                    totalRecords += count;
                    log.info("Market index daily cleanse: {} → {} records", indexCode, count);
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to cleanse market index daily for {}: {}", indexCode, e.getMessage());
            }
        }
        return totalRecords;
    }

    private Long executeSectorIndexFetch(String dataType, String sectorType, LocalDate startDate, LocalDate endDate) {
        DataCollectionLog fetchLog = createLog(dataType, "FETCH", endDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        int successCount;
        try {
            successCount = fetchSectorIndexDaily(sectorType, startDate, endDate, fetchLog.getId());
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

    int fetchSectorIndexDaily(String sectorType, LocalDate startDate, LocalDate endDate) {
        return fetchSectorIndexDaily(sectorType, startDate, endDate, null);
    }

    int fetchSectorIndexDaily(String sectorType, LocalDate startDate, LocalDate endDate, Long collectionLogId) {
        String startStr = startDate != null ? startDate.format(YMD) : "";
        String endStr = endDate != null ? endDate.format(YMD) : "";

        if ("INDUSTRY".equals(sectorType)) {
            List<Industry> sectors = industryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Industry>()
                            .eq(Industry::getIsDeleted, false)
            );
            if (sectors.isEmpty()) {
                throw new IllegalStateException("行业表为空，请先采集行业名称（INDUSTRY_NAME）");
            }
            int total = 0;
            for (Industry sector : sectors) {
                try {
                    String rawJson = aktoolsClient.fetchIndustryIndexDaily(sector.getName(), startStr, endStr);
                    if (rawJson != null && !rawJson.equals("[]")) {
                        saveSectorRawData(collectionLogId, "INDUSTRY_INDEX_DAILY", endDate, sector.getCode(), rawJson);
                        total++;
                    }
                    aktoolsClient.sleepBetweenCalls();
                } catch (Exception e) {
                    log.error("Failed to fetch industry index daily for {}: {}", sector.getCode(), e.getMessage());
                }
            }
            return total;
        } else {
            List<Concept> sectors = conceptMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Concept>()
                            .eq(Concept::getIsDeleted, false)
            );
            if (sectors.isEmpty()) {
                throw new IllegalStateException("概念表为空，请先采集概念名称（CONCEPT_NAME）");
            }
            int total = 0;
            for (Concept sector : sectors) {
                try {
                    String rawJson = aktoolsClient.fetchConceptIndexDaily(sector.getName(), startStr, endStr);
                    if (rawJson != null && !rawJson.equals("[]")) {
                        saveSectorRawData(collectionLogId, "CONCEPT_INDEX_DAILY", endDate, sector.getCode(), rawJson);
                        total++;
                    }
                    aktoolsClient.sleepBetweenCalls();
                } catch (Exception e) {
                    log.error("Failed to fetch concept index daily for {}: {}", sector.getCode(), e.getMessage());
                }
            }
            return total;
        }
    }

    private void saveSectorRawData(Long collectionLogId, String dataType, LocalDate tradeDate, String sectorCode, String rawJson) {
        RawData rawData = new RawData();
        rawData.setCollectionLogId(collectionLogId);
        rawData.setDataType(dataType);
        rawData.setTradeDate(tradeDate);
        rawData.setSource("AKTools");
        rawData.setSectorCode(sectorCode);
        rawData.setRawJson(rawJson);
        rawData.setFetchAt(LocalDateTime.now());
        rawDataMapper.insert(rawData);
    }

    private int cleanseAllSectorIndices(String sectorType, LocalDate tradeDate) {
        String dataType = "INDUSTRY".equals(sectorType) ? "INDUSTRY_INDEX_DAILY" : "CONCEPT_INDEX_DAILY";

        List<RawData> rawDatas = rawDataMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RawData>()
                        .eq(RawData::getDataType, dataType)
                        .eq(RawData::getTradeDate, tradeDate)
                        .isNotNull(RawData::getSectorCode)
        );

        if (rawDatas.isEmpty()) {
            log.warn("No raw_data found for {} on {}, falling back to direct API fetch", dataType, tradeDate);
            return cleanseAllSectorIndicesDirect(sectorType, tradeDate);
        }

        int total = 0;
        for (RawData rd : rawDatas) {
            try {
                String rawJson = rd.getRawJson();
                if (rawJson != null && !rawJson.equals("[]")) {
                    total += sectorIndexDailyCleanseService.cleanse(rawJson, sectorType, rd.getSectorCode());
                }
            } catch (Exception e) {
                log.error("Failed to cleanse sector index daily for {}: {}", rd.getSectorCode(), e.getMessage());
            }
        }
        return total;
    }

    private int cleanseAllSectorIndicesDirect(String sectorType, LocalDate tradeDate) {
        String startDate = tradeDate != null ? tradeDate.format(YMD) : "";
        String endDate = startDate;

        if ("INDUSTRY".equals(sectorType)) {
            List<Industry> sectors = industryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Industry>()
                            .eq(Industry::getIsDeleted, false)
            );
            int total = 0;
            for (Industry sector : sectors) {
                try {
                    String rawJson = aktoolsClient.fetchIndustryIndexDaily(sector.getName(), startDate, endDate);
                    if (rawJson != null && !rawJson.equals("[]")) {
                        total += sectorIndexDailyCleanseService.cleanse(rawJson, "INDUSTRY", sector.getCode());
                    }
                    aktoolsClient.sleepBetweenCalls();
                } catch (Exception e) {
                    log.error("Failed to cleanse industry index daily for {}: {}", sector.getCode(), e.getMessage());
                }
            }
            return total;
        } else {
            List<Concept> sectors = conceptMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Concept>()
                            .eq(Concept::getIsDeleted, false)
            );
            int total = 0;
            for (Concept sector : sectors) {
                try {
                    String rawJson = aktoolsClient.fetchConceptIndexDaily(sector.getName(), startDate, endDate);
                    if (rawJson != null && !rawJson.equals("[]")) {
                        total += sectorIndexDailyCleanseService.cleanse(rawJson, "CONCEPT", sector.getCode());
                    }
                    aktoolsClient.sleepBetweenCalls();
                } catch (Exception e) {
                    log.error("Failed to cleanse concept index daily for {}: {}", sector.getCode(), e.getMessage());
                }
            }
            return total;
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
     * 估算原始 JSON 中的记录数量。
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
        log.info("Starting stock daily backfill (Tushare): {} to {}", startDate, endDate);

        int totalRecords = 0;
        int failedDays = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            try {
                String resp = tushareClient.fetchDaily(date);
                int count = stockDailyCleanseService.cleanseTushareDaily(resp);
                totalRecords += count;
                log.info("Tushare daily backfill: {} → {} records", date, count);
            } catch (Exception e) {
                log.error("Tushare daily backfill failed for {}", date, e);
                failedDays++;
            }
            date = date.plusDays(1);
        }

        String result = String.format("日线补采完成: %d 条记录，%d 天失败",
                totalRecords, failedDays);
        log.info(result);
        return result;
    }

    @Async("collectionExecutor")
    public CompletableFuture<String> backfillSectorIndexDailyAsync(String sectorType, LocalDate startDate, LocalDate endDate) {
        String result = backfillSectorIndexDaily(sectorType, startDate, endDate);
        return CompletableFuture.completedFuture(result);
    }

    public String backfillSectorIndexDaily(String sectorType, LocalDate startDate, LocalDate endDate) {
        log.info("Starting sector index daily backfill: sectorType={}, {} to {}", sectorType, startDate, endDate);

        String dataType = "INDUSTRY".equals(sectorType) ? "INDUSTRY_INDEX_DAILY" : "CONCEPT_INDEX_DAILY";
        Long fetchLogId = executeSectorIndexFetch(dataType, sectorType, startDate, endDate);
        if (fetchLogId == null) {
            return "板块指数K线补采失败: 无板块数据";
        }

        int cleanseCount = 0;
        List<RawData> rawDatas = rawDataMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RawData>()
                        .eq(RawData::getCollectionLogId, fetchLogId)
        );
        for (RawData rd : rawDatas) {
            try {
                String rawJson = rd.getRawJson();
                if (rawJson != null && !rawJson.equals("[]")) {
                    cleanseCount += sectorIndexDailyCleanseService.cleanse(rawJson, sectorType, rd.getSectorCode());
                }
            } catch (Exception e) {
                log.error("Failed to cleanse sector index daily for {}: {}", rd.getSectorCode(), e.getMessage());
            }
        }

        String result = String.format("板块指数K线补采完成: %d 个板块，%d 条记录",
                rawDatas.size(), cleanseCount);
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
     * 检查给定日期是否已有成功的 FETCH 和 CLEANSE 日志。
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