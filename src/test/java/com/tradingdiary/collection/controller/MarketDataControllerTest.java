package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.ConceptIndustryVO;
import com.tradingdiary.service.MarketDataService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private MarketDataController marketDataController;

    @Test
    void shouldReturnConcepts() {
        ConceptIndustryVO vo = new ConceptIndustryVO();
        vo.setCode("GN001");
        vo.setName("MSCI");
        vo.setStockCount(268);
        vo.setMarginBalance(new BigDecimal("68050000000.00"));
        vo.setMarginChange(new BigDecimal("1520000000.00"));
        vo.setShortBalance(new BigDecimal("4580000000.00"));
        vo.setShortChange(new BigDecimal("-310000000.00"));
        vo.setSnapDate(LocalDate.of(2026, 5, 18));

        when(marketDataService.listConcepts(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("records", List.of(vo), "total", 1L));

        var response = marketDataController.concepts(null, null, "marginBalance", "desc", 1, 50);

        assertThat(response.getCode()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<ConceptIndustryVO> records = (List<ConceptIndustryVO>) response.getData().get("records");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getName()).isEqualTo("MSCI");
        assertThat(records.get(0).getStockCount()).isEqualTo(268);
    }

    @Test
    void shouldReturnIndustries() {
        ConceptIndustryVO vo = new ConceptIndustryVO();
        vo.setCode("HY001");
        vo.setName("银行");
        vo.setStockCount(42);

        when(marketDataService.listIndustries(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Map.of("records", List.of(vo), "total", 1L));

        var response = marketDataController.industries(null, null, "stockCount", "asc", 1, 20);

        assertThat(response.getCode()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<ConceptIndustryVO> records = (List<ConceptIndustryVO>) response.getData().get("records");
        assertThat(records.get(0).getName()).isEqualTo("银行");
    }
}
