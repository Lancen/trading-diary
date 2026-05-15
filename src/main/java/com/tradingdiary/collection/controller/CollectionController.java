package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.GapDetectionService;
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
        DATA_TYPE_LABELS.put("STOCK_INFO", "股票基础信息");
        DATA_TYPE_LABELS.put("STOCK_DAILY", "股票日线行情");
        DATA_TYPE_LABELS.put("TRADE_CALENDAR", "交易日历");
        DATA_TYPE_LABELS.put("INDUSTRY_NAME", "行业板块分类");
        DATA_TYPE_LABELS.put("INDUSTRY_CONS", "行业成分股");
        DATA_TYPE_LABELS.put("CONCEPT_NAME", "概念板块分类");
        DATA_TYPE_LABELS.put("CONCEPT_CONS", "概念成分股");
        DATA_TYPE_LABELS.put("MARGIN_DAILY_SSE", "两融明细(沪市)");
        DATA_TYPE_LABELS.put("MARGIN_DAILY_SZSE", "两融明细(深市)");
    }

    private final DataCollectionLogMapper logMapper;
    private final GapDetectionService gapDetectionService;
    private final CollectionOrchestrator orchestrator;

    public CollectionController(DataCollectionLogMapper logMapper,
                                 GapDetectionService gapDetectionService,
                                 CollectionOrchestrator orchestrator) {
        this.logMapper = logMapper;
        this.gapDetectionService = gapDetectionService;
        this.orchestrator = orchestrator;
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
            return ApiResponse.fail(400, "Unknown data type: " + dataType);
        }
        String result = orchestrator.orchestrate(dataType, LocalDate.now());
        return ApiResponse.ok(result);
    }

    @PostMapping("/backfill")
    public ApiResponse<String> backfill(@RequestBody BackfillRequest request) {
        if (request.getDataType() == null || request.getDataType().isBlank()) {
            return ApiResponse.fail(400, "dataType is required");
        }
        if (request.getStartDate() == null) {
            return ApiResponse.fail(400, "startDate is required");
        }
        if (request.getEndDate() == null) {
            return ApiResponse.fail(400, "endDate is required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            return ApiResponse.fail(400, "endDate must be on or after startDate");
        }
        String result = orchestrator.backfillMarginByWeek(request);
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
