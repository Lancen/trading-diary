package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.MarginMacroCleanseService;
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
 * MarginMacroSzseHandler 单元测试，验证深市两融总量采集的 fetch 和 cleanse 委托
 */
@ExtendWith(MockitoExtension.class)
class MarginMacroSzseHandlerTest {

    private AKToolsClient aktoolsClient;
    private MarginMacroCleanseService marginMacroCleanseService;
    private MarginMacroSzseHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        marginMacroCleanseService = mock(MarginMacroCleanseService.class);
        handler = new MarginMacroSzseHandler(aktoolsClient, marginMacroCleanseService);
    }

    // 测试流程: 验证 dataType 返回 MARGIN_MACRO_SZSE
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("MARGIN_MACRO_SZSE");
    }

    // 测试流程: Given aktoolsClient 返回总量数据, When 调用 fetch, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateFetchToClient() {
        when(aktoolsClient.fetchMacroMarginSz()).thenReturn("[{...}]");
        String result = handler.fetch(LocalDate.now());
        assertThat(result).isEqualTo("[{...}]");
        verify(aktoolsClient).fetchMacroMarginSz();
    }

    // 测试流程: Given marginMacroCleanseService 返回 1, When 调用 cleanse, Then 以 SZSE 交易所标识委托并返回结果
    @Test
    void shouldDelegateCleanseWithSZSE() {
        when(marginMacroCleanseService.cleanse("[{...}]", "SZSE")).thenReturn(1);
        int count = handler.cleanse("[{...}]", LocalDate.now());
        assertThat(count).isEqualTo(1);
    }
}