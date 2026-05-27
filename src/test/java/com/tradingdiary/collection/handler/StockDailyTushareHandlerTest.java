package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.TushareClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StockDailyTushareHandler 单元测试，验证 Tushare 日线数据的 fetch 和 cleanse 委托
 */
@ExtendWith(MockitoExtension.class)
class StockDailyTushareHandlerTest {

    private TushareClient tushareClient;
    private StockDailyCleanseService stockDailyCleanseService;
    private StockDailyTushareHandler handler;

    @BeforeEach
    void setUp() {
        tushareClient = mock(TushareClient.class);
        stockDailyCleanseService = mock(StockDailyCleanseService.class);
        handler = new StockDailyTushareHandler(tushareClient, stockDailyCleanseService);
    }

    // 测试流程: 验证 dataType 返回 STOCK_DAILY_TUSHARE
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("STOCK_DAILY_TUSHARE");
    }

    // 测试流程: Given tushareClient 返回日线数据, When 调用 fetch, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateFetchToTushareClient() {
        LocalDate date = LocalDate.of(2026, 5, 20);
        when(tushareClient.fetchDaily(date)).thenReturn("{\"data\":{}}");
        FetchResult result = handler.fetch(date);
        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
        assertThat(result.getRawJson()).isEqualTo("{\"data\":{}}");
        verify(tushareClient).fetchDaily(date);
    }

    // 测试流程: Given stockDailyCleanseService 返回 5000, When 调用 cleanse, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateCleanseToService() {
        when(stockDailyCleanseService.cleanseTushareDaily("{\"data\":{}}")).thenReturn(5000);
        int count = handler.cleanse("{\"data\":{}}", LocalDate.of(2026, 5, 20));
        assertThat(count).isEqualTo(5000);
        verify(stockDailyCleanseService).cleanseTushareDaily("{\"data\":{}}");
    }
}