package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.security.JwtAuthFilter;
import com.tradingdiary.service.CalendarService;
import com.tradingdiary.service.StockDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 股票数据控制器单元测试（collection 包）
 */
@WebMvcTest(StockDataController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class StockDataControllerTest {

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private StockDataService stockDataService;

    @MockBean
    private CalendarService calendarService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnStockList() throws Exception {
        StockListVO vo = new StockListVO();
        vo.setStockCode("000001");
        vo.setStockName("平安银行");
        vo.setClose(new BigDecimal("12.80"));
        vo.setChangePct(new BigDecimal("2.40"));

        when(stockDataService.listStocks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("records", List.of(vo), "total", 1L));

        mockMvc.perform(get("/api/v1/admin/stocks/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].stockCode").value("000001"))
                .andExpect(jsonPath("$.data.records[0].stockName").value("平安银行"));
    }

    @Test
    void shouldReturnStockListWithFilters() throws Exception {
        when(stockDataService.listStocks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("records", List.of(), "total", 0L));

        mockMvc.perform(get("/api/v1/admin/stocks/list")
                        .param("keyword", "平安")
                        .param("industry", "银行")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldReturnCalendar() throws Exception {
        when(calendarService.getMonthCalendar(anyInt(), anyInt(), any()))
                .thenReturn(Map.of("yearMonth", "2026-05", "days", List.of()));

        mockMvc.perform(get("/api/v1/admin/stocks/calendar")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.yearMonth").value("2026-05"));
    }
}
