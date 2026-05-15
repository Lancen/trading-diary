package com.tradingdiary.collection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.security.JwtAuthFilter;
import com.tradingdiary.service.GapDetectionService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataCollectionLogMapper logMapper;

    @MockBean
    private GapDetectionService gapDetectionService;

    @MockBean
    private CollectionOrchestrator orchestrator;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    // ─── GET /api/v1/admin/collection/status ──────────────────────────

    @Test
    void shouldReturnStatusWithNineCards() throws Exception {
        when(logMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(9))
                .andExpect(jsonPath("$.data[0].dataType").value("STOCK_INFO"))
                .andExpect(jsonPath("$.data[1].dataType").value("STOCK_DAILY"))
                .andExpect(jsonPath("$.data[2].dataType").value("TRADE_CALENDAR"))
                .andExpect(jsonPath("$.data[3].dataType").value("INDUSTRY_NAME"))
                .andExpect(jsonPath("$.data[4].dataType").value("INDUSTRY_CONS"))
                .andExpect(jsonPath("$.data[5].dataType").value("CONCEPT_NAME"))
                .andExpect(jsonPath("$.data[6].dataType").value("CONCEPT_CONS"))
                .andExpect(jsonPath("$.data[7].dataType").value("MARGIN_DAILY_SSE"))
                .andExpect(jsonPath("$.data[8].dataType").value("MARGIN_DAILY_SZSE"));
    }

    @Test
    void shouldReturnStatusWithSuccessfulLogs() throws Exception {
        DataCollectionLog fetchLog = buildLog("MARGIN_DAILY_SSE", "FETCH", "SUCCESS", 100);
        DataCollectionLog cleanseLog = buildLog("MARGIN_DAILY_SSE", "CLEANSE", "SUCCESS", 100);

        when(logMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);
        when(logMapper.selectLatestByDataTypeAndJobType("MARGIN_DAILY_SSE", "FETCH"))
                .thenReturn(fetchLog);
        when(logMapper.selectLatestByDataTypeAndJobType("MARGIN_DAILY_SSE", "CLEANSE"))
                .thenReturn(cleanseLog);

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[7].dataType").value("MARGIN_DAILY_SSE"))
                .andExpect(jsonPath("$.data[7].lastFetch.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[7].lastCleanse.status").value("SUCCESS"));
    }

    @Test
    void shouldReturnStatusWithFailedLogs() throws Exception {
        DataCollectionLog fetchLog = buildLog("STOCK_DAILY", "FETCH", "FAILED", 0);
        fetchLog.setErrorMsg("Connection timeout");

        when(logMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);
        when(logMapper.selectLatestByDataTypeAndJobType("STOCK_DAILY", "FETCH"))
                .thenReturn(fetchLog);

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].dataType").value("STOCK_DAILY"))
                .andExpect(jsonPath("$.data[1].lastFetch.status").value("FAILED"))
                .andExpect(jsonPath("$.data[1].lastFetch.errorMsg").value("Connection timeout"))
                .andExpect(jsonPath("$.data[1].lastCleanse").doesNotExist());
    }

    // ─── GET /api/v1/admin/collection/logs ────────────────────────────

    @Test
    void shouldReturnLogs() throws Exception {
        List<DataCollectionLog> logs = List.of(
                buildLog("STOCK_INFO", "FETCH", "SUCCESS", 5000),
                buildLog("STOCK_INFO", "CLEANSE", "SUCCESS", 5000)
        );

        when(logMapper.selectRecentByDataType("STOCK_INFO", 10))
                .thenReturn(logs);

        mockMvc.perform(get("/api/v1/admin/collection/logs")
                        .param("dataType", "STOCK_INFO")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dataType").value("STOCK_INFO"))
                .andExpect(jsonPath("$.data[0].jobType").value("FETCH"));
    }

    @Test
    void shouldReturnLogsWithDefaultValues() throws Exception {
        when(logMapper.selectRecentByDataType("STOCK_INFO", 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/collection/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ─── GET /api/v1/admin/collection/gaps ────────────────────────────

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
                "SSE"))
                .thenReturn(gapReport);

        mockMvc.perform(get("/api/v1/admin/collection/gaps")
                        .param("start", "2026-05-04")
                        .param("end", "2026-05-15")
                        .param("exchange", "SSE"))
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
                "SSE"))
                .thenReturn(gapReport);

        mockMvc.perform(get("/api/v1/admin/collection/gaps")
                        .param("start", "2026-05-04")
                        .param("end", "2026-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalWeeks").value(0));
    }

    // ─── POST /api/v1/admin/collection/trigger/{dataType} ─────────────

    @Test
    void shouldTriggerOrchestrationForValidDataType() throws Exception {
        when(orchestrator.orchestrate(eq("STOCK_INFO"), any()))
                .thenReturn("Collection started");

        mockMvc.perform(post("/api/v1/admin/collection/trigger/STOCK_INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Collection started"));
    }

    @Test
    void shouldReturn400ForInvalidDataTypeOnTrigger() throws Exception {
        mockMvc.perform(post("/api/v1/admin/collection/trigger/UNKNOWN_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Unknown data type: UNKNOWN_TYPE"));
    }

    // ─── POST /api/v1/admin/collection/backfill ───────────────────────

    @Test
    void shouldBackfillWithValidRequest() throws Exception {
        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 8));

        when(orchestrator.backfillMarginByWeek(any(BackfillRequest.class)))
                .thenReturn("Backfill complete: 5 dates processed");

        mockMvc.perform(post("/api/v1/admin/collection/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Backfill complete: 5 dates processed"));
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
                .andExpect(jsonPath("$.message").value("dataType is required"));
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
                .andExpect(jsonPath("$.message").value("startDate is required"));
    }

    @Test
    void shouldReturn400ForMissingEndDateOnBackfill() throws Exception {
        String body = """
                {
                    "dataType": "MARGIN_DAILY_SSE",
                    "exchange": "SSE",
                    "startDate": "2026-05-04"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/collection/backfill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("endDate is required"));
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
                .andExpect(jsonPath("$.message").value("endDate must be on or after startDate"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

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
