package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.ConceptCleanseService;
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
 * ConceptNameHandler 单元测试，验证概念板块名称采集的 fetch 和 cleanse 委托
 */
@ExtendWith(MockitoExtension.class)
class ConceptNameHandlerTest {

    private AKToolsClient aktoolsClient;
    private ConceptCleanseService conceptCleanseService;
    private ConceptNameHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        conceptCleanseService = mock(ConceptCleanseService.class);
        handler = new ConceptNameHandler(aktoolsClient, conceptCleanseService);
    }

    // 测试流程: 验证 dataType 返回 CONCEPT_NAME
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("CONCEPT_NAME");
    }

    // 测试流程: Given aktoolsClient 返回概念名称数据, When 调用 fetch, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateFetchToClient() {
        when(aktoolsClient.fetchConceptNames()).thenReturn("[{...}]");
        FetchResult result = handler.fetch(LocalDate.now());
        assertThat(result.getRawJson()).isEqualTo("[{...}]");
        assertThat(result.getType()).isEqualTo(FetchResult.Type.SINGLE);
        verify(aktoolsClient).fetchConceptNames();
    }

    // 测试流程: Given conceptCleanseService 返回 300, When 调用 cleanse, Then 返回委托结果并验证调用
    @Test
    void shouldDelegateCleanseToService() {
        when(conceptCleanseService.cleanseNames("[{...}]")).thenReturn(300);
        int count = handler.cleanse("[{...}]", LocalDate.now());
        assertThat(count).isEqualTo(300);
        verify(conceptCleanseService).cleanseNames("[{...}]");
    }
}