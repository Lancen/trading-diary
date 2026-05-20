package com.tradingdiary.collection.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.GapDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tradingdiary.security.JwtAuthFilter;
import com.tradingdiary.service.collection.ConstituentImportService;
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

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

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
    private TradeCalendarMapper tradeCalendarMapper;

    @MockBean
    private ConstituentImportService constituentImportService;

    private static final String[] EXPECTED_TYPES = {
            "STOCK_INFO", "TRADE_CALENDAR", "INDUSTRY_NAME", "CONCEPT_NAME",
            "MARGIN_DAILY_SSE", "MARGIN_DAILY_SZSE", "MARGIN_MACRO_SSE", "MARGIN_MACRO_SZSE"
    };

    @Test
    void shouldReturnStatusWithEightCards() throws Exception {
        when(logMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/admin/collection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].dataType").value("STOCK_INFO"));
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
                .andExpect(jsonPath("$.data[4].dataType").value("MARGIN_DAILY_SSE"))
                .andExpect(jsonPath("$.data[4].lastFetch.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[4].lastCleanse.status").value("SUCCESS"));
    }

    @Test
    void shouldReturnStatusWithFailedLogs() throws Exception {
        DataCollectionLog fetchLog = buildLog("MARGIN_DAILY_SSE", "FETCH", "FAILED", 0);
        fetchLog.setErrorMsg("Connection timeout");

        when(logMapper.selectLatestByDataTypeAndJobType(anyString(), anyString()))
                .thenReturn(null);
        when(logMapper.selectLatestByDataTypeAndJobType("MARGIN_DAILY_SSE", "FETCH"))
                .thenReturn(fetchLog);

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

        when(logMapper.selectRecentByDataType("STOCK_INFO", 10))
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
        when(logMapper.selectRecentByDataType("STOCK_INFO", 10))
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

    @Test
    void shouldTriggerOrchestrationForValidDataType() throws Exception {
        TradeCalendar cal = new TradeCalendar();
        cal.setTradeDate(LocalDate.of(2026, 5, 20));
        cal.setIsTradingDay(1);
        when(tradeCalendarMapper.selectOne(any()))
                .thenReturn(cal);

        mockMvc.perform(post("/api/v1/admin/collection/trigger/STOCK_INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("任务已提交（交易日: 2026-05-20），正在后台执行"));
    }

    @Test
    void shouldReturn400ForInvalidDataTypeOnTrigger() throws Exception {
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
