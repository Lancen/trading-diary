package com.tradingdiary.controller;

import com.tradingdiary.collection.controller.StockDataController;
import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.collection.model.StockDetailVO;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.StockDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDataControllerTest {

    @Mock
    private StockDataService stockDataService;

    @InjectMocks
    private StockDataController stockDataController;

    @Test
    void shouldReturnStockListWithDefaultParams() {
        StockListVO vo = new StockListVO();
        vo.setStockCode("000001");
        vo.setStockName("平安银行");
        vo.setIndustry("银行");
        vo.setConcepts("MSCI,沪深300");
        vo.setClose(new BigDecimal("12.80"));
        vo.setChangePct(new BigDecimal("2.40"));
        vo.setVolume(52340100L);
        vo.setMarginBalance(new BigDecimal("3258000000.00"));
        vo.setMarginChange(new BigDecimal("123000000.00"));
        vo.setShortBalance(new BigDecimal("15000000.00"));
        vo.setShortChange(new BigDecimal("-5000000.00"));
        vo.setTradeDate(LocalDate.of(2026, 5, 20));

        when(stockDataService.listStocks(any(), any(), any(),
                any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("records", List.of(vo), "total", 1L));

        ApiResponse<Map<String, Object>> response = stockDataController.list(
                null, null, null, null, null, "desc", 1, 50);

        assertThat(response.getCode()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<StockListVO> records = (List<StockListVO>) response.getData().get("records");
        assertThat(records).hasSize(1);
        StockListVO first = records.get(0);
        assertThat(first.getStockCode()).isEqualTo("000001");
        assertThat(first.getStockName()).isEqualTo("平安银行");
        assertThat(first.getMarginChange().compareTo(BigDecimal.ZERO)).isPositive();
    }

    @Test
    void shouldReturnStockListWithFilters() {
        when(stockDataService.listStocks(eq("平安"), eq("银行"), eq("MSCI"),
                any(), eq("changePct"), eq("asc"), eq(1), eq(20)))
                .thenReturn(Map.of("records", List.of(), "total", 0L));

        ApiResponse<Map<String, Object>> response = stockDataController.list(
                "平安", "银行", "MSCI", null, "changePct", "asc", 1, 20);

        assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    void shouldReturnStockDetail() {
        StockDetailVO detail = new StockDetailVO();
        detail.setStockCode("000001");
        detail.setStockName("平安银行");
        detail.setIndustry("银行");
        detail.setConcepts(List.of("MSCI", "沪深300"));

        StockDetailVO.LatestQuote quote = new StockDetailVO.LatestQuote();
        quote.setClose(new BigDecimal("12.80"));
        quote.setChangePct(new BigDecimal("2.40"));
        detail.setLatestQuote(quote);

        when(stockDataService.getStockDetail(eq("000001"), any(), any()))
                .thenReturn(detail);

        ApiResponse<StockDetailVO> response = stockDataController.detail("000001", null, null);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getStockCode()).isEqualTo("000001");
        assertThat(response.getData().getConcepts()).contains("MSCI");
    }
}
