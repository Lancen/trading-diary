package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.MarginCleanseService;
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
 * MarginDailySseHandler 单元测试，验证沪市两融明细采集的 fetch 日期格式化和 cleanse 委托
 */
@ExtendWith(MockitoExtension.class)
class MarginDailySseHandlerTest {

    private AKToolsClient aktoolsClient;
    private MarginCleanseService marginCleanseService;
    private MarginDailySseHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        marginCleanseService = mock(MarginCleanseService.class);
        handler = new MarginDailySseHandler(aktoolsClient, marginCleanseService);
    }

    // 测试流程: 验证 dataType 返回 MARGIN_DAILY_SSE
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("MARGIN_DAILY_SSE");
    }

    // 测试流程: Given 日期 2026-05-20, When 调用 fetch, Then 以 yyyyMMdd 格式传入 client 并返回结果
    @Test
    void shouldDelegateFetchWithDateStr() {
        LocalDate date = LocalDate.of(2026, 5, 20);
        when(aktoolsClient.fetchMarginDetailSse("20260520")).thenReturn("[{...}]");
        String result = handler.fetch(date);
        assertThat(result).isEqualTo("[{...}]");
        verify(aktoolsClient).fetchMarginDetailSse("20260520");
    }

    // 测试流程: Given marginCleanseService 返回 500, When 调用 cleanse, Then 以 SSE 交易所标识委托并返回结果
    @Test
    void shouldDelegateCleanseWithSSE() {
        LocalDate date = LocalDate.of(2026, 5, 20);
        when(marginCleanseService.cleanse("[{...}]", "SSE", date)).thenReturn(500);
        int count = handler.cleanse("[{...}]", date);
        assertThat(count).isEqualTo(500);
    }
}