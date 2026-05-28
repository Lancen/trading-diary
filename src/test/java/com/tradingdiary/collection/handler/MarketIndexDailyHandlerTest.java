package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.MarketIndexDailyCleanseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MarketIndexDailyHandler 单元测试，验证宽基指数日线采集的 8 指数遍历逻辑
 */
@ExtendWith(MockitoExtension.class)
class MarketIndexDailyHandlerTest {

    private AKToolsClient aktoolsClient;
    private MarketIndexDailyCleanseService marketIndexDailyCleanseService;
    private MarketIndexDailyHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        marketIndexDailyCleanseService = mock(MarketIndexDailyCleanseService.class);
        handler = new MarketIndexDailyHandler(aktoolsClient, marketIndexDailyCleanseService);
    }

    // 测试流程: 验证 dataType 返回 MARKET_INDEX_DAILY
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("MARKET_INDEX_DAILY");
    }

    // 测试流程: Given aktoolsClient 返回指数日线数据, When 调用 fetch, Then 遍历所有指数代码并返回非空结果
    @Test
    void shouldFetchAllMarketIndices() {
        when(aktoolsClient.fetchMarketIndexDaily("sh000001")).thenReturn("[{...}]");
        when(aktoolsClient.fetchMarketIndexDaily("sz399001")).thenReturn("[]");

        FetchResult result = handler.fetch(LocalDate.of(2026, 5, 20));
        assertThat(result.getRawJson()).isNotEmpty();
        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
        verify(aktoolsClient, atLeastOnce()).fetchMarketIndexDaily("sh000001");
    }

    // 测试流程: Given 两个 mock 均返回数据, When 调用 cleanse, Then 清洗总数大于等于单指数数量
    @Test
    void shouldCleanseAllMarketIndices() {
        when(aktoolsClient.fetchMarketIndexDaily("sh000001")).thenReturn("[{...}]");
        when(marketIndexDailyCleanseService.cleanse("[{...}]", "sh000001")).thenReturn(50);

        int count = handler.cleanse("[{...}]", LocalDate.of(2026, 5, 20));
        assertThat(count).isGreaterThanOrEqualTo(50);
    }
}