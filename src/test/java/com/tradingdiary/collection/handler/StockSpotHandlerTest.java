package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import com.tradingdiary.service.collection.StockInfoCleanseService;
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
 * StockSpotHandler 单元测试，验证股票行情采集的 fetch 委托和 cleanse 双表写入
 */
@ExtendWith(MockitoExtension.class)
class StockSpotHandlerTest {

    private AKToolsClient aktoolsClient;
    private StockInfoCleanseService stockInfoCleanseService;
    private StockDailyCleanseService stockDailyCleanseService;
    private StockSpotHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        stockInfoCleanseService = mock(StockInfoCleanseService.class);
        stockDailyCleanseService = mock(StockDailyCleanseService.class);
        handler = new StockSpotHandler(aktoolsClient, stockInfoCleanseService, stockDailyCleanseService);
    }

    // 测试流程: 验证 dataType 返回 STOCK_SPOT
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("STOCK_SPOT");
    }

    // 测试流程: Given aktoolsClient 返回行情数据, When 调用 fetch, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateFetchToClient() {
        when(aktoolsClient.fetchStockSpot()).thenReturn("[{...}]");
        FetchResult result = handler.fetch(LocalDate.of(2026, 5, 20));
        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
        assertThat(result.getRawJson()).isEqualTo("[{...}]");
        verify(aktoolsClient).fetchStockSpot();
    }

    // 测试流程: Given 两个 cleanseService 各返回 3000, When 调用 cleanse, Then 合计返回 6000（双表写入）
    @Test
    void shouldCleanseBothInfoAndDaily() {
        when(stockInfoCleanseService.cleanse("[{...}]", LocalDate.of(2026, 5, 20))).thenReturn(3000);
        when(stockDailyCleanseService.cleanse("[{...}]", LocalDate.of(2026, 5, 20))).thenReturn(3000);
        int count = handler.cleanse("[{...}]", LocalDate.of(2026, 5, 20));
        assertThat(count).isEqualTo(6000);
    }
}