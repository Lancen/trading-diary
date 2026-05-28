package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.TradeCalendarService;
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
 * TradeCalendarHandler 单元测试，验证交易日历采集的 fetch 和 cleanse 委托
 */
@ExtendWith(MockitoExtension.class)
class TradeCalendarHandlerTest {

    private AKToolsClient aktoolsClient;
    private TradeCalendarService tradeCalendarService;
    private TradeCalendarHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        tradeCalendarService = mock(TradeCalendarService.class);
        handler = new TradeCalendarHandler(aktoolsClient, tradeCalendarService);
    }

    // 测试流程: 验证 dataType 返回 TRADE_CALENDAR
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("TRADE_CALENDAR");
    }

    // 测试流程: Given aktoolsClient 返回日历数据, When 调用 fetch, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateFetchToClient() {
        when(aktoolsClient.fetchTradeCalendar()).thenReturn("[{...}]");
        FetchResult result = handler.fetch(LocalDate.now());
        assertThat(result.getRawJson()).isEqualTo("[{...}]");
        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
        verify(aktoolsClient).fetchTradeCalendar();
    }

    // 测试流程: Given tradeCalendarService 返回 1200, When 调用 cleanse, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateCleanseToService() {
        when(tradeCalendarService.syncTradeCalendar()).thenReturn(1200);
        int count = handler.cleanse("[{...}]", LocalDate.now());
        assertThat(count).isEqualTo(1200);
        verify(tradeCalendarService).syncTradeCalendar();
    }
}