package com.tradingdiary.collection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.security.JwtAuthFilter;
import com.tradingdiary.service.GapDetectionService;
import com.tradingdiary.service.collection.CollectionQueryService;
import com.tradingdiary.service.collection.ConstituentImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class CollectionControllerTest {

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CollectionQueryService collectionQueryService;

    @MockBean
    private GapDetectionService gapDetectionService;

    @MockBean
    private CollectionOrchestrator orchestrator;

    @MockBean
    private ConstituentImportService constituentImportService;

    @Test
    void shouldReturnStatusWithEightCards() throws Exception {
        when(collectionQueryService.getCollectionStatus())
                .thenReturn(buildStatusList());

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].dataType").value("STOCK_INFO"));
    }

    @Test
    void shouldReturnStatusWithSuccessfulLogs() throws Exception {
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

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[4].dataType").value("MARGIN_DAILY_SSE"))
                .andExpect(jsonPath("$.data[4].lastFetch.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[4].lastCleanse.status").value("SUCCESS"));
    }

    @Test
    void shouldReturnStatusWithFailedLogs() throws Exception {
        List<CollectionStatusVO> statusList = buildStatusList();
        CollectionStatusVO marginDailySse = statusList.get(4);
        CollectionStatusVO.JobStatus fetchStatus = new CollectionStatusVO.JobStatus();
        fetchStatus.setStatus("FAILED");
        fetchStatus.setRecordCount(0);
        fetchStatus.setErrorMsg("Connection timeout");
        marginDailySse.setLastFetch(fetchStatus);

        when(collectionQueryService.getCollectionStatus())
                .thenReturn(statusList);

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[4].lastFetch.status").value("FAILED"))
                .andExpect(jsonPath("$.data[4].lastFetch.errorMsg").value("Connection timeout"));
    }

    @Test
    void shouldReturnLogs() throws Exception {
        List<DataCollectionLog> logs = List.of(
                buildLog("STOCK_INFO", "FETCH", "SUCCESS", 5000),
                buildLog("STOCK_INFO", "CLEANSE", "SUCCESS", 5000)
        );

        when(collectionQueryService.getRecentLogs("STOCK_INFO", 10))
                .thenReturn(logs);

        mockMvc.perform(get("/api/v1/admin/collection/logs")
                        .param("dataType", "STOCK_INFO")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dataType").value("STOCK_INFO"))
                .andExpect(jsonPath("$.data[0].jobType").value("FETCH"));
    }

    @Test
    void shouldReturnLogsWithDefaultValues() throws Exception {
        when(collectionQueryService.getRecentLogs("STOCK_INFO", 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/collection/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturnGapsReport() throws Exception {
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

        when(gapDetectionService.getGaps(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 15),
                "MARGIN_DAILY_SSE"))
                .thenReturn(gapReport);

        mockMvc.perform(get("/api/v1/admin/collection/gaps")
                        .param("start", "2026-05-04")
                        .param("end", "2026-05-15")
                        .param("dataType", "MARGIN_DAILY_SSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalWeeks").value(1))
                .andExpect(jsonPath("$.data.completeWeeks").value(1))
                .andExpect(jsonPath("$.data.weeks[0].status").value("COMPLETE"));
    }

    @Test
    void shouldReturnGapsWithDefaultExchange() throws Exception {
        GapReportVO gapReport = new GapReportVO();
        gapReport.setWeeks(List.of());
        gapReport.setTotalWeeks(0);
        gapReport.setCompleteWeeks(0);
        gapReport.setPartialWeeks(0);
        gapReport.setMissingWeeks(0);

        when(gapDetectionService.getGaps(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 15),
                "MARGIN_DAILY_SSE"))
                .thenReturn(gapReport);

        mockMvc.perform(get("/api/v1/admin/collection/gaps")
                        .param("start", "2026-05-04")
                        .param("end", "2026-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalWeeks").value(0));
    }

    @Test
    void shouldTriggerOrchestrationForValidDataType() throws Exception {
        when(collectionQueryService.isValidDataType("STOCK_INFO")).thenReturn(true);
        when(collectionQueryService.getLatestTradeDate()).thenReturn(LocalDate.of(2026, 5, 20));

        mockMvc.perform(post("/api/v1/admin/collection/trigger/STOCK_INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("任务已提交（交易日: 2026-05-20），正在后台执行"));
    }

    @Test
    void shouldReturn400ForInvalidDataTypeOnTrigger() throws Exception {
        when(collectionQueryService.isValidDataType("UNKNOWN_TYPE")).thenReturn(false);

        mockMvc.perform(post("/api/v1/admin/collection/trigger/UNKNOWN_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("未知数据类型: UNKNOWN_TYPE"));
    }

    @Test
    void shouldBackfillWithValidRequest() throws Exception {
        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 8));

        mockMvc.perform(post("/api/v1/admin/collection/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("补采任务已提交，正在后台执行"));
    }

    @Test
    void shouldReturn400ForMissingDataTypeOnBackfill() throws Exception {
        String body = """
                {
                    "exchange": "SSE",
                    "startDate": "2026-05-04",
                    "endDate": "2026-05-08"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/collection/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("dataType 不能为空"));
    }

    @Test
    void shouldReturn400ForMissingStartDateOnBackfill() throws Exception {
        String body = """
                {
                    "dataType": "MARGIN_DAILY_SSE",
                    "exchange": "SSE",
                    "endDate": "2026-05-08"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/collection/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("startDate 不能为空"));
    }

    @Test
    void shouldReturn400ForEndDateBeforeStartDateOnBackfill() throws Exception {
        String body = """
                {
                    "dataType": "MARGIN_DAILY_SSE",
                    "exchange": "SSE",
                    "startDate": "2026-05-08",
                    "endDate": "2026-05-04"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/collection/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("endDate 不能早于 startDate"));
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

    private static DataCollectionLog buildLog(String dataType, String jobType, String status, int recordCount) {
        DataCollectionLog log = new DataCollectionLog();
        log.setId(1L);
        log.setDataType(dataType);
        log.setJobType(jobType);
        log.setStatus(status);
        log.setRecordCount(recordCount);
        log.setStartedAt(LocalDateTime.now().minusMinutes(5));
        log.setCompletedAt(LocalDateTime.now());
        return log;
    }
}
