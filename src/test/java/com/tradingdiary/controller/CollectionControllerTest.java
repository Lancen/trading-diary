package com.tradingdiary.controller;

import com.tradingdiary.collection.controller.CollectionController;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.collection.ConstituentImportService;
import com.tradingdiary.service.GapDetectionService;
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
    private DataCollectionLogMapper dataCollectionLogMapper;

    @Mock
    private GapDetectionService gapDetectionService;

    @Mock
    private CollectionOrchestrator collectionOrchestrator;

    @Mock
    private TradeCalendarMapper tradeCalendarMapper;

    @Mock
    private StockInfoMapper stockInfoMapper;

    @Mock
    private IndustryMapper industryMapper;

    @Mock
    private ConceptMapper conceptMapper;

    @Mock
    private MarginDailyMapper marginDailyMapper;

    @Mock
    private MarginMacroMapper marginMacroMapper;

    @Mock
    private ConstituentImportService constituentImportService;

    @InjectMocks
    private CollectionController collectionController;

    @Test
    void shouldReturnStatusForAllDataTypesWhenNoLogsExist() {
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType(anyString(), eq("FETCH")))
                .thenReturn(null);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType(anyString(), eq("CLEANSE")))
                .thenReturn(null);

        ApiResponse<List<CollectionStatusVO>> response = collectionController.status();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).hasSize(8);

        CollectionStatusVO first = response.getData().get(0);
        assertThat(first.getDataType()).isEqualTo("STOCK_INFO");
        assertThat(first.getDataTypeLabel()).isNotNull();
    }

    @Test
    void shouldReturnStatusWithSuccessfulLogs() {
        DataCollectionLog fetchLog = buildLog("MARGIN_DAILY_SSE", "FETCH", "SUCCESS", 100);
        DataCollectionLog cleanseLog = buildLog("MARGIN_DAILY_SSE", "CLEANSE", "SUCCESS", 100);

        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType("MARGIN_DAILY_SSE", "FETCH"))
                .thenReturn(fetchLog);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType("MARGIN_DAILY_SSE", "CLEANSE"))
                .thenReturn(cleanseLog);

        ApiResponse<List<CollectionStatusVO>> response = collectionController.status();

        CollectionStatusVO sse = response.getData().stream()
                .filter(v -> "MARGIN_DAILY_SSE".equals(v.getDataType()))
                .findFirst().orElseThrow();
        assertThat(sse.getLastFetch().getStatus()).isEqualTo("SUCCESS");
        assertThat(sse.getLastCleanse().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldReturnStatusWithFailedLogs() {
        DataCollectionLog fetchLog = buildLog("MARGIN_DAILY_SSE", "FETCH", "FAILED", 0);
        fetchLog.setErrorMsg("Connection timeout");

        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobType("MARGIN_DAILY_SSE", "FETCH"))
                .thenReturn(fetchLog);

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

        when(dataCollectionLogMapper.selectRecentByDataType("STOCK_INFO", 10))
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
        com.tradingdiary.entity.TradeCalendar cal = new com.tradingdiary.entity.TradeCalendar();
        cal.setTradeDate(LocalDate.of(2026, 5, 20));
        cal.setIsTradingDay(1);
        when(tradeCalendarMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(cal);

        ApiResponse<String> response = collectionController.trigger("STOCK_INFO");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).contains("任务已提交");
    }

    @Test
    void shouldReturnErrorForInvalidDataTypeOnTrigger() {
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

        when(dataCollectionLogMapper.selectRecentByDataType("STOCK_INFO", 10))
                .thenReturn(logs);

        ApiResponse<List<DataCollectionLog>> response = collectionController.logs("STOCK_INFO", 10);

        assertThat(response.getCode()).isEqualTo(200);
        DataCollectionLog result = response.getData().get(0);
        assertThat(result.getRequestUrl()).isEqualTo("http://aktools:8080/api/public/stock_zh_a_hist_tx");
        assertThat(result.getRequestParams()).isEqualTo("symbol=000001&start_date=2026-05-20&end_date=2026-05-20");
        assertThat(result.getRemark()).isEqualTo("腾讯行情API采集");
    }
}
