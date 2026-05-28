package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.IndustryCleanseService;
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
 * IndustryNameHandler 单元测试，验证行业板块名称采集的 fetch 和 cleanse 委托
 */
@ExtendWith(MockitoExtension.class)
class IndustryNameHandlerTest {

    private AKToolsClient aktoolsClient;
    private IndustryCleanseService industryCleanseService;
    private IndustryNameHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        industryCleanseService = mock(IndustryCleanseService.class);
        handler = new IndustryNameHandler(aktoolsClient, industryCleanseService);
    }

    // 测试流程: 验证 dataType 返回 INDUSTRY_NAME
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("INDUSTRY_NAME");
    }

    // 测试流程: Given aktoolsClient 返回行业名称数据, When 调用 fetch, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateFetchToClient() {
        when(aktoolsClient.fetchIndustryNames()).thenReturn("[{...}]");
        FetchResult result = handler.fetch(LocalDate.now());
        assertThat(result.getRawJson()).isEqualTo("[{...}]");
        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
        verify(aktoolsClient).fetchIndustryNames();
    }

    // 测试流程: Given industryCleanseService 返回 80, When 调用 cleanse, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateCleanseToService() {
        when(industryCleanseService.cleanseNames("[{...}]")).thenReturn(80);
        int count = handler.cleanse("[{...}]", LocalDate.now());
        assertThat(count).isEqualTo(80);
        verify(industryCleanseService).cleanseNames("[{...}]");
    }
}