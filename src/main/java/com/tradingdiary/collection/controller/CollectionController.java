package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.GapDetectionService;
import com.tradingdiary.service.collection.CollectionQueryService;
import com.tradingdiary.service.collection.ConstituentImportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据采集控制器，管理采集状态查询、任务触发、缺口检测和历史数据补采
 */
@RestController
@RequestMapping("/api/v1/admin/collection")
@PreAuthorize("hasRole('ADMIN')")
public class CollectionController {

    private final CollectionQueryService collectionQueryService;
    private final GapDetectionService gapDetectionService;
    private final CollectionOrchestrator orchestrator;
    private final ConstituentImportService constituentImportService;

    public CollectionController(CollectionQueryService collectionQueryService,
                                 GapDetectionService gapDetectionService,
                                 CollectionOrchestrator orchestrator,
                                 ConstituentImportService constituentImportService) {
        this.collectionQueryService = collectionQueryService;
        this.gapDetectionService = gapDetectionService;
        this.orchestrator = orchestrator;
        this.constituentImportService = constituentImportService;
    }

    @Operation(summary = "获取所有数据类型的采集状态")
    @GetMapping("/status")
    public ApiResponse<List<CollectionStatusVO>> status() {
        List<CollectionStatusVO> statusList = collectionQueryService.getCollectionStatus();
        return ApiResponse.ok(statusList);
    }

    /**
     * 获取指定数据类型的采集日志
     * <p>
     * 按时间倒序返回指定数据类型的最近采集日志记录。
     * </p>
     *
     * @param dataType 数据类型，默认为 "STOCK_INFO"
     * @param limit 返回记录数，默认为 10
     * @return 采集日志列表
     */
    @Operation(summary = "获取指定数据类型的采集日志")
    @GetMapping("/logs")
    public ApiResponse<List<DataCollectionLog>> logs(
            @RequestParam(defaultValue = "STOCK_INFO") String dataType,
            @RequestParam(defaultValue = "10") int limit) {
        List<DataCollectionLog> logs = collectionQueryService.getRecentLogs(dataType, limit);
        return ApiResponse.ok(logs);
    }

    /**
     * 检测指定日期范围内的数据缺口
     * <p>
     * 对比交易日历和实际采集的两融数据，返回缺失的交易日数据。
     * 结果按周分组，便于查看每周的数据完整性。
     * </p>
     *
     * @param start 开始日期
     * @param end 结束日期
     * @param dataType 数据类型（MARGIN_DAILY_SSE/SZSE 或 MARGIN_MACRO_SSE/SZSE），默认 MARGIN_DAILY_SSE
     * @return 数据缺口报告，包含每周的完整度统计和缺失日期列表
     */
    @Operation(summary = "检测指定日期范围内的数据缺口")
    @GetMapping("/gaps")
    public ApiResponse<GapReportVO> gaps(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam(defaultValue = "MARGIN_DAILY_SSE") String dataType) {
        GapReportVO report = gapDetectionService.getGaps(start, end, dataType);
        return ApiResponse.ok(report);
    }

    /**
     * 触发指定数据类型的采集任务
     * <p>
     * 异步执行数据采集任务，自动使用最近交易日作为采集日期。
     * </p>
     *
     * @param dataType 数据类型
     * @return 任务提交结果
     */
    @Operation(summary = "触发指定数据类型的采集任务")
    @PostMapping("/trigger/{dataType}")
    public ApiResponse<String> trigger(@PathVariable String dataType) {
        if (!collectionQueryService.isValidDataType(dataType)) {
            return ApiResponse.fail(400, "未知数据类型: " + dataType);
        }
        LocalDate tradeDate = collectionQueryService.getLatestTradeDate();
        new Thread(() -> {
            String result = orchestrator.orchestrate(dataType, tradeDate);
            log.info("异步采集完成: {} {} → {}", dataType, tradeDate, result);
        }, "collection-" + dataType).start();
        return ApiResponse.ok("任务已提交（交易日: " + tradeDate + "），正在后台执行");
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CollectionController.class);

    /**
     * 执行历史数据补采
     * <p>
     * 异步执行指定日期范围内的数据补采任务，支持股票日线和两融数据。
     * </p>
     *
     * @param request 补采请求参数
     * @return 任务提交结果
     */
    @Operation(summary = "执行历史数据补采")
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

    /**
     * 获取可用的成分股数据文件列表
     *
     * @return 成分股文件列表，包含文件名、大小、修改时间等信息
     */
    @Operation(summary = "获取可用的成分股数据文件列表")
    @GetMapping("/constituents/files")
    public ApiResponse<List<Map<String, Object>>> constituentFiles() {
        return ApiResponse.ok(constituentImportService.listFiles());
    }

    /**
     * 导入成分股数据文件
     * <p>
     * 从指定的JSON文件中导入行业和概念成分股数据到数据库。
     * </p>
     *
     * @param body 请求体，包含filename字段
     * @return 导入结果，包含导入的关系数量和状态
     */
    @Operation(summary = "导入成分股数据文件")
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
}
