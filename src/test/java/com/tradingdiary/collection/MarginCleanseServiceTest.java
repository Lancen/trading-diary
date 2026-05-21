package com.tradingdiary.collection;

import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginStockMapper;
import com.tradingdiary.service.collection.MarginCleanseService;
import com.tradingdiary.util.BatchSqlRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarginCleanseServiceTest {

    @Mock private MarginDailyMapper marginDailyMapper;
    @Mock private MarginStockMapper marginStockMapper;
    @Mock private BatchSqlRunner batchSqlRunner;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private MarginCleanseService service;

    @Test
    void shouldCalculateMarginChangeCorrectly() throws Exception {
        LocalDate today = LocalDate.of(2026, 5, 15);
        String rawJson = """
                [
                    {"标的证券代码":"000001","融资余额":"100000000.00","融资买入额":"5000000.00","融券余额":"1000000.00","融券卖出量":"1000","融券余量":"5000"}
                ]
                """;
        when(objectMapper.readTree(rawJson)).thenReturn(new ObjectMapper().readTree(rawJson));

        MarginDaily prevDay = new MarginDaily();
        prevDay.setStockCode("000001");
        prevDay.setTradeDate(LocalDate.of(2026, 5, 14));
        prevDay.setMarginBalance(new BigDecimal("95000000.00"));
        prevDay.setShortBalance(new BigDecimal("900000.00"));

        // Mock: 上一交易日查询
        when(marginDailyMapper.selectList(any()))
                .thenReturn(List.of(prevDay))  // first call: findPreviousTradeDate
                .thenReturn(List.of(prevDay))  // second call: prev day data
                .thenReturn(List.of());         // third call: existing data check

        when(batchSqlRunner.batchInsert(any())).thenReturn(1);

        service.cleanse(rawJson, "SSE", today);

        ArgumentCaptor<List<MarginDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(batchSqlRunner).batchInsert(captor.capture());
        List<MarginDaily> inserted = captor.getValue();

        assertThat(inserted).hasSize(1);
        MarginDaily result = inserted.get(0);
        assertThat(result.getMarginChange()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(result.getShortChange()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void shouldSetChangeToNullWhenNoPreviousData() throws Exception {
        LocalDate today = LocalDate.of(2026, 5, 15);
        String rawJson = """
                [
                    {"标的证券代码":"000001","融资余额":"100000000.00","融资买入额":"5000000.00","融券余额":"1000000.00","融券卖出量":"1000","融券余量":"5000"}
                ]
                """;
        when(objectMapper.readTree(rawJson)).thenReturn(new ObjectMapper().readTree(rawJson));

        // Mock: findPreviousTradeDate returns empty (no prev trading day)
        when(marginDailyMapper.selectList(any()))
                .thenReturn(List.of())   // findPreviousTradeDate: empty
                .thenReturn(List.of());  // existing data check: empty

        when(batchSqlRunner.batchInsert(any())).thenReturn(1);

        service.cleanse(rawJson, "SSE", today);

        ArgumentCaptor<List<MarginDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(batchSqlRunner).batchInsert(captor.capture());
        List<MarginDaily> inserted = captor.getValue();

        assertThat(inserted.get(0).getMarginChange()).isNull();
        assertThat(inserted.get(0).getShortChange()).isNull();
    }
}
