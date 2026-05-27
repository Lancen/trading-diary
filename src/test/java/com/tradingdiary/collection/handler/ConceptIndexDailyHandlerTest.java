package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.service.collection.SectorIndexDailyCleanseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConceptIndexDailyHandler 单元测试，验证概念指数日线采集的 fetchSectors 和 cleanse 流程
 */
@ExtendWith(MockitoExtension.class)
class ConceptIndexDailyHandlerTest {

    private AKToolsClient aktoolsClient;
    private SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private ConceptMapper conceptMapper;
    private RawDataMapper rawDataMapper;
    private ConceptIndexDailyHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        sectorIndexDailyCleanseService = mock(SectorIndexDailyCleanseService.class);
        conceptMapper = mock(ConceptMapper.class);
        rawDataMapper = mock(RawDataMapper.class);
        handler = new ConceptIndexDailyHandler(aktoolsClient, sectorIndexDailyCleanseService, conceptMapper, rawDataMapper);
    }

    // 测试流程: 验证 dataType 返回 CONCEPT_INDEX_DAILY
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("CONCEPT_INDEX_DAILY");
    }

    // 测试流程: Given 概念指数不支持标准 fetch, When 调用 fetch, Then 抛出 UnsupportedOperationException
    @Test
    void shouldThrowOnStandardFetch() {
        assertThatThrownBy(() -> handler.fetch(LocalDate.now()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // 测试流程: Given concept 表为空, When 调用 fetchSectors, Then 抛出 IllegalStateException
    @Test
    void shouldThrowWhenConceptTableEmpty() {
        when(conceptMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> handler.fetchSectors(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20), 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // 测试流程: Given concept 表有板块数据且 client 返回行情, When 调用 fetchSectors, Then 返回 1 并保存 rawData
    @Test
    void shouldFetchAndSaveSectors() {
        Concept sector = new Concept();
        sector.setCode("BK0500");
        sector.setName("人工智能");
        when(conceptMapper.selectList(any())).thenReturn(List.of(sector));
        when(aktoolsClient.fetchConceptIndexDaily(eq("人工智能"), any(), any())).thenReturn("[{...}]");

        int count = handler.fetchSectors(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20), 1L);
        assertThat(count).isEqualTo(1);
        verify(rawDataMapper).insert(any(RawData.class));
    }

    // 测试流程: Given rawData 中有概念指数数据, When 调用 cleanse, Then 以 CONCEPT 类型委托清洗并返回 25
    @Test
    void shouldCleanseFromRawData() {
        RawData rd = new RawData();
        rd.setRawJson("[{...}]");
        rd.setSectorCode("BK0500");
        when(rawDataMapper.selectList(any())).thenReturn(List.of(rd));
        when(sectorIndexDailyCleanseService.cleanse("[{...}]", "CONCEPT", "BK0500")).thenReturn(25);

        int count = handler.cleanse("[{...}]", LocalDate.of(2026, 5, 20));
        assertThat(count).isEqualTo(25);
    }
}