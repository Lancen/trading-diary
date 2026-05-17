package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.GapDetectionService;
import com.tradingdiary.service.collection.ConstituentImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/collection")
@PreAuthorize("hasRole('ADMIN')")
public class CollectionController {

    private static final Map<String, String> DATA_TYPE_LABELS = new LinkedHashMap<>();

    static {
        DATA_TYPE_LABELS.put("STOCK_INFO", "股票行情（含日线）");
        DATA_TYPE_LABELS.put("TRADE_CALENDAR", "交易日历");
        DATA_TYPE_LABELS.put("INDUSTRY_NAME", "行业板块分类");
        DATA_TYPE_LABELS.put("CONCEPT_NAME", "概念板块分类");
        DATA_TYPE_LABELS.put("MARGIN_DAILY_SSE", "两融明细(沪市)");
        DATA_TYPE_LABELS.put("MARGIN_DAILY_SZSE", "两融明细(深市)");
    }

    private final DataCollectionLogMapper logMapper;
    private final GapDetectionService gapDetectionService;
    private final CollectionOrchestrator orchestrator;
    private final ConstituentImportService constituentImportService;
    private final TradeCalendarMapper tradeCalendarMapper;

    public CollectionController(DataCollectionLogMapper logMapper,
                                 GapDetectionService gapDetectionService,
                                 CollectionOrchestrator orchestrator,
                                 ConstituentImportService constituentImportService,
                                 TradeCalendarMapper tradeCalendarMapper) {
        this.logMapper = logMapper;
        this.gapDetectionService = gapDetectionService;
        this.orchestrator = orchestrator;
        this.constituentImportService = constituentImportService;
        this.tradeCalendarMapper = tradeCalendarMapper;
    }

    @GetMapping("/status")
    public ApiResponse<List<CollectionStatusVO>> status() {
        List<CollectionStatusVO> statusList = new ArrayList<>();

        for (Map.Entry<String, String> entry : DATA_TYPE_LABELS.entrySet()) {
            String dataType = entry.getKey();
            String label = entry.getValue();

            DataCollectionLog fetchLog = logMapper.selectLatestByDataTypeAndJobType(dataType, "FETCH");
            DataCollectionLog cleanseLog = logMapper.selectLatestByDataTypeAndJobType(dataType, "CLEANSE");

            CollectionStatusVO vo = new CollectionStatusVO();
            vo.setDataType(dataType);
            vo.setDataTypeLabel(label);
            vo.setLastFetch(buildJobStatus(fetchLog));
            vo.setLastCleanse(buildJobStatus(cleanseLog));
            statusList.add(vo);
        }

        return ApiResponse.ok(statusList);
    }

    @GetMapping("/logs")
    public ApiResponse<List<DataCollectionLog>> logs(
            @RequestParam(defaultValue = "STOCK_INFO") String dataType,
            @RequestParam(defaultValue = "10") int limit) {
        List<DataCollectionLog> logs = logMapper.selectRecentByDataType(dataType, limit);
        return ApiResponse.ok(logs);
    }

    @GetMapping("/gaps")
    public ApiResponse<GapReportVO> gaps(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam(defaultValue = "SSE") String exchange) {
        GapReportVO report = gapDetectionService.getGaps(start, end, exchange);
        return ApiResponse.ok(report);
    }

    @PostMapping("/trigger/{dataType}")
    public ApiResponse<String> trigger(@PathVariable String dataType) {
        if (!DATA_TYPE_LABELS.containsKey(dataType)) {
            return ApiResponse.fail(400, "未知数据类型: " + dataType);
        }
        // 取最近交易日（非交易日无数据）
        LocalDate tradeDate = getLatestTradeDate();
        new Thread(() -> {
            String result = orchestrator.orchestrate(dataType, tradeDate);
            log.info("异步采集完成: {} {} → {}", dataType, tradeDate, result);
        }, "collection-" + dataType).start();
        return ApiResponse.ok("任务已提交（交易日: " + tradeDate + "），正在后台执行");
    }

    private LocalDate getLatestTradeDate() {
        TradeCalendar cal = tradeCalendarMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeCalendar>()
                        .eq(TradeCalendar::getIsTradingDay, 1)
                        .le(TradeCalendar::getTradeDate, LocalDate.now())
                        .orderByDesc(TradeCalendar::getTradeDate)
                        .last("LIMIT 1")
        );
        return cal != null ? cal.getTradeDate() : LocalDate.now();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CollectionController.class);

    @PostMapping("/backfill")
    public ApiResponse<String> backfill(@RequestBody BackfillRequest request) {
        if (request.getDataType() == null || request.getDataType().isBlank()) {
            return ApiResponse.fail(400, "dataType 不能为空");
        }
        if (request.getStartDate() == null) {
            return ApiResponse.fail(400, "startDate 不能为空");
        }
        if (request.getEndDate() == null) {
            return ApiResponse.fail(400, "endDate 不能为空");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            return ApiResponse.fail(400, "endDate 不能早于 startDate");
        }
        // 异步执行补采（耗时可能很长）
        final String dt = request.getDataType();
        final String exchange = request.getExchange();
        final LocalDate start = request.getStartDate();
        final LocalDate end = request.getEndDate();
        new Thread(() -> {
            String result;
            if ("STOCK_DAILY".equals(dt)) {
                result = orchestrator.backfillStockDaily(start, end);
            } else {
                com.tradingdiary.collection.model.BackfillRequest req = new com.tradingdiary.collection.model.BackfillRequest();
                req.setDataType(dt);
                req.setExchange(exchange);
                req.setStartDate(start);
                req.setEndDate(end);
                result = orchestrator.backfillMarginByWeek(req);
            }
            log.info("异步补采完成: {} {}~{} → {}", dt, start, end, result);
        }, "backfill-" + dt).start();
        return ApiResponse.ok("补采任务已提交，正在后台执行");
    }

    @GetMapping("/constituents/files")
    public ApiResponse<List<Map<String, Object>>> constituentFiles() {
        return ApiResponse.ok(constituentImportService.listFiles());
    }

    @PostMapping("/constituents/import")
    public ApiResponse<Map<String, Object>> importConstituents(@RequestBody Map<String, String> body) {
        String filename = body.get("filename");
        if (filename == null || filename.isBlank()) {
            return ApiResponse.fail(400, "filename 不能为空");
        }
        Map<String, Object> result = constituentImportService.importFromFile(filename);
        if ("failed".equals(result.get("status"))) {
            return ApiResponse.fail(500, "导入失败: " + result.get("error"));
        }
        return ApiResponse.ok(result);
    }

    private CollectionStatusVO.JobStatus buildJobStatus(DataCollectionLog log) {
        if (log == null) {
            return null;
        }
        CollectionStatusVO.JobStatus js = new CollectionStatusVO.JobStatus();
        js.setStatus(log.getStatus());
        js.setStartedAt(log.getStartedAt());
        js.setCompletedAt(log.getCompletedAt());
        js.setRecordCount(log.getRecordCount());
        js.setErrorMsg(log.getErrorMsg());
        return js;
    }
}
