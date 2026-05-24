package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.MarginSummaryVO;
import com.tradingdiary.security.JwtAuthFilter;
import com.tradingdiary.service.collection.MarginStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 两融统计控制器单元测试
 */
@WebMvcTest(MarginStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class MarginStatsControllerTest {

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private MarginStatsService marginStatsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnMarginSummary() throws Exception {
        MarginSummaryVO vo = new MarginSummaryVO();
        vo.setTotalMarginBalance(new BigDecimal("150000000000"));
        vo.setTotalShortBalance(new BigDecimal("12000000000"));
        vo.setTotalBalance(new BigDecimal("162000000000"));
        vo.setStockCount(2800);
        vo.setTradeDate(LocalDate.of(2026, 5, 20));

        when(marginStatsService.getMarginSummary(any())).thenReturn(vo);

        mockMvc.perform(get("/api/v1/admin/margin-stats/summary")
                        .param("tradeDate", "2026-05-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalMarginBalance").value("150000000000"))
                .andExpect(jsonPath("$.data.stockCount").value(2800))
                .andExpect(jsonPath("$.data.tradeDate").value("2026-05-20"));
    }

    @Test
    void shouldReturnSummaryWithoutTradeDate() throws Exception {
        MarginSummaryVO vo = new MarginSummaryVO();
        vo.setTotalMarginBalance(new BigDecimal("150000000000"));
        vo.setTotalShortBalance(new BigDecimal("12000000000"));
        vo.setTotalBalance(new BigDecimal("162000000000"));
        vo.setStockCount(2800);

        when(marginStatsService.getMarginSummary(any())).thenReturn(vo);

        mockMvc.perform(get("/api/v1/admin/margin-stats/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalMarginBalance").value("150000000000"));
    }
}
