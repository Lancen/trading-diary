package com.tradingdiary.controller;

import com.tradingdiary.collection.controller.CollectionController;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.collection.ConstituentImportService;
import com.tradingdiary.service.GapDetectionService;
import com.tradingdiary.service.collection.CollectionQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    @Mock
    private CollectionQueryService collectionQueryService;

    @Mock
    private GapDetectionService gapDetectionService;

    @Mock
    private CollectionOrchestrator collectionOrchestrator;

    @Mock
    private ConstituentImportService constituentImportService;

    @InjectMocks
    private CollectionController collectionController;

    @Test
    void shouldReturnStatusForAllDataTypesWhenNoLogsExist() {
        when(collectionQueryService.getCollectionStatus())
                .thenReturn(buildStatusList());

        ApiResponse<List<CollectionStatusVO>> response = collectionController.status();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(8);

        CollectionStatusVO first = response.getData().get(0);
        assertThat(first.getDataType()).isEqualTo("STOCK_INFO");
        assertThat(first.getDataTypeLabel()).isNotNull();
    }

    @Test
    void shouldReturnStatusWithSuccessfulLogs() {
        List<CollectionStatusVO> statusList = buildStatusList();
        CollectionStatusVO marginDailySse = statusList.get(4);
        CollectionStatusVO.JobStatus fetchStatus = new CollectionStatusVO.JobStatus();
        fetchStatus.setStatus("SUCCESS");
        fetchStatus.setRecordCount(100);
        CollectionStatusVO.JobStatus cleanseStatus = new CollectionStatusVO.JobStatus();
        cleanseStatus.setStatus("SUCCESS");
        cleanseStatus.setRecordCount(100);
        marginDailySse.setLastFetch(fetchStatus);
        marginDailySse.setLastCleanse(cleanseStatus);

        when(collectionQueryService.getCollectionStatus())
                .thenReturn(statusList);

        ApiResponse<List<CollectionStatusVO>> response = collectionController.status();

        CollectionStatusVO sse = response.getData().stream()
                .filter(v -> "MARGIN_DAILY_SSE".equals(v.getDataType()))
                .findFirst().orElseThrow();
        assertThat(sse.getLastFetch().getStatus()).isEqualTo("SUCCESS");
        assertThat(sse.getLastCleanse().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldReturnStatusWithFailedLogs() {
        List<CollectionStatusVO> statusList = buildStatusList();
        CollectionStatusVO marginDailySse = statusList.get(4);
        CollectionStatusVO.JobStatus fetchStatus = new CollectionStatusVO.JobStatus();
        fetchStatus.setStatus("FAILED");
        fetchStatus.setRecordCount(0);
        fetchStatus.setErrorMsg("Connection timeout");
        marginDailySse.setLastFetch(fetchStatus);

        when(collectionQueryService.getCollectionStatus())
                .thenReturn(statusList);

        ApiResponse<List<CollectionStatusVO>> response = collectionController.status();

        CollectionStatusVO item = response.getData().stream()
                .filter(v -> "MARGIN_DAILY_SSE".equals(v.getDataType()))
                .findFirst().orElseThrow();
        assertThat(item.getLastFetch().getStatus()).isEqualTo("FAILED");
        assertThat(item.getLastFetch().getErrorMsg()).isEqualTo("Connection timeout");
        assertThat(item.getLastCleanse()).isNull();
    }

    @Test
    void shouldReturnRecentLogs() {
        List<DataCollectionLog> logs = new ArrayList<>();
        logs.add(buildLog("STOCK_INFO", "FETCH", "SUCCESS", 5000));
        logs.add(buildLog("STOCK_INFO", "CLEANSE", "SUCCESS", 5000));

        when(collectionQueryService.getRecentLogs("STOCK_INFO", 10))
                .thenReturn(logs);

        ApiResponse<List<DataCollectionLog>> response = collectionController.logs("STOCK_INFO", 10);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    void shouldReturnGapsReport() {
        LocalDate start = LocalDate.of(2026, 5, 4);
        LocalDate end = LocalDate.of(2026, 5, 15);

        GapReportVO gapReport = new GapReportVO();
        GapReportVO.WeekGap weekGap = new GapReportVO.WeekGap();
        weekGap.setWeekStart(LocalDate.of(2026, 5, 4));
        weekGap.setWeekEnd(LocalDate.of(2026, 5, 8));
        weekGap.setExpectedDays(5);
        weekGap.setCollectedDays(5);
        weekGap.setMissingDates(List.of());
        weekGap.setStatus("COMPLETE");
        gapReport.setWeeks(List.of(weekGap));
        gapReport.setTotalWeeks(1);
        gapReport.setCompleteWeeks(1);
        gapReport.setPartialWeeks(0);
        gapReport.setMissingWeeks(0);

        when(gapDetectionService.getGaps(start, end, "MARGIN_DAILY_SSE")).thenReturn(gapReport);

        ApiResponse<GapReportVO> response = collectionController.gaps(start, end, "MARGIN_DAILY_SSE");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getCompleteWeeks()).isEqualTo(1);
    }

    @Test
    void shouldReturnGapsWithDefaultExchange() {
        LocalDate start = LocalDate.of(2026, 5, 4);
        LocalDate end = LocalDate.of(2026, 5, 15);

        GapReportVO gapReport = new GapReportVO();
        gapReport.setWeeks(List.of());
        gapReport.setTotalWeeks(0);
        gapReport.setCompleteWeeks(0);
        gapReport.setPartialWeeks(0);
        gapReport.setMissingWeeks(0);

        when(gapDetectionService.getGaps(start, end, "MARGIN_DAILY_SSE")).thenReturn(gapReport);

        ApiResponse<GapReportVO> response = collectionController.gaps(start, end, "MARGIN_DAILY_SSE");

        assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    void shouldTriggerOrchestrationForValidDataType() {
        when(collectionQueryService.isValidDataType("STOCK_INFO")).thenReturn(true);
        when(collectionQueryService.getLatestTradeDate()).thenReturn(LocalDate.of(2026, 5, 20));

        ApiResponse<String> response = collectionController.trigger("STOCK_INFO");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).contains("任务已提交");
    }

    @Test
    void shouldReturnErrorForInvalidDataTypeOnTrigger() {
        when(collectionQueryService.isValidDataType("UNKNOWN_TYPE")).thenReturn(false);

        ApiResponse<String> response = collectionController.trigger("UNKNOWN_TYPE");

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("未知数据类型");
    }

    @Test
    void shouldBackfillWithValidRequest() {
        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 8));

        ApiResponse<String> response = collectionController.backfill(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).contains("补采");
    }

    @Test
    void shouldReturnErrorForMissingDataTypeOnBackfill() {
        BackfillRequest request = new BackfillRequest();
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 8));

        ApiResponse<String> response = collectionController.backfill(request);

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("dataType");
    }

    @Test
    void shouldReturnErrorForMissingStartDateOnBackfill() {
        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setEndDate(LocalDate.of(2026, 5, 8));

        ApiResponse<String> response = collectionController.backfill(request);

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("startDate");
    }

    @Test
    void shouldReturnErrorForEndDateBeforeStartDateOnBackfill() {
        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 8));
        request.setEndDate(LocalDate.of(2026, 5, 4));

        ApiResponse<String> response = collectionController.backfill(request);

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage()).contains("endDate");
    }

    private DataCollectionLog buildLog(String dataType, String jobType, String status, int recordCount) {
        DataCollectionLog log = new DataCollectionLog();
        log.setId(1L);
        log.setDataType(dataType);
        log.setJobType(jobType);
        log.setStatus(status);
        log.setRecordCount(recordCount);
        log.setRequestUrl("http://aktools:8080/api/public/stock_zh_a_hist_tx");
        log.setRequestParams("symbol=000001&start_date=2026-05-20&end_date=2026-05-20");
        log.setRemark("全量A股行情快照");
        log.setStartedAt(LocalDateTime.now().minusMinutes(5));
        log.setCompletedAt(LocalDateTime.now());
        return log;
    }

    @Test
    void shouldReturnLogsWithNewFields() {
        List<DataCollectionLog> logs = new ArrayList<>();
        DataCollectionLog log = buildLog("STOCK_INFO", "FETCH", "SUCCESS", 5000);
        log.setRemark("腾讯行情API采集");
        logs.add(log);

        when(collectionQueryService.getRecentLogs("STOCK_INFO", 10))
                .thenReturn(logs);

        ApiResponse<List<DataCollectionLog>> response = collectionController.logs("STOCK_INFO", 10);

        assertThat(response.getCode()).isEqualTo(200);
        DataCollectionLog result = response.getData().get(0);
        assertThat(result.getRequestUrl()).isEqualTo("http://aktools:8080/api/public/stock_zh_a_hist_tx");
        assertThat(result.getRequestParams()).isEqualTo("symbol=000001&start_date=2026-05-20&end_date=2026-05-20");
        assertThat(result.getRemark()).isEqualTo("腾讯行情API采集");
    }

    private static List<CollectionStatusVO> buildStatusList() {
        String[] types = {
                "STOCK_INFO", "TRADE_CALENDAR", "INDUSTRY_NAME", "CONCEPT_NAME",
                "MARGIN_DAILY_SSE", "MARGIN_DAILY_SZSE", "MARGIN_MACRO_SSE", "MARGIN_MACRO_SZSE"
        };
        String[] labels = {
                "股票行情（含日线）", "交易日历", "行业板块分类", "概念板块分类",
                "两融明细(沪市)", "两融明细(深市)", "两融总量(沪市)", "两融总量(深市)"
        };
        List<CollectionStatusVO> list = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            CollectionStatusVO vo = new CollectionStatusVO();
            vo.setDataType(types[i]);
            vo.setDataTypeLabel(labels[i]);
            list.add(vo);
        }
        return list;
    }
}
