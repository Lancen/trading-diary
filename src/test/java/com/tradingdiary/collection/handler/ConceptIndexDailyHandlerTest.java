package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConceptIndexDailyHandler 单元测试，验证概念指数日线采集的 fetch 和 cleanse 流程
 */
@ExtendWith(MockitoExtension.class)
class ConceptIndexDailyHandlerTest {

    private AKToolsClient aktoolsClient;
    private SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private ConceptMapper conceptMapper;
    private RawDataMapper rawDataMapper;
    private DataCollectionLogMapper logMapper;
    private ConceptIndexDailyHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        sectorIndexDailyCleanseService = mock(SectorIndexDailyCleanseService.class);
        conceptMapper = mock(ConceptMapper.class);
        rawDataMapper = mock(RawDataMapper.class);
        logMapper = mock(DataCollectionLogMapper.class);
        handler = new ConceptIndexDailyHandler(aktoolsClient, sectorIndexDailyCleanseService,
                conceptMapper, rawDataMapper, logMapper);
    }

    // 测试流程: 验证 dataType() 返回 CONCEPT_INDEX_DAILY
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("CONCEPT_INDEX_DAILY");
    }

    // 测试流程: Given concept 表为空, When 调用 fetch, Then 返回失败结果且日志标记 FAILED
    @Test
    void shouldReturnFailedResultWhenConceptTableEmpty() {
        when(conceptMapper.selectList(any())).thenReturn(Collections.emptyList());
        FetchResult result = handler.fetch(LocalDate.of(2026, 5, 20));
        assertThat(result.isSuccess()).isFalse();
        verify(logMapper).insert(any(DataCollectionLog.class));
        verify(logMapper).updateById(any(DataCollectionLog.class));
    }

    // 测试流程: Given concept 表有板块数据且 client 返回行情, When 调用 fetch, Then 返回 FetchResult.multiSector 且保存 rawData
    @Test
    void shouldFetchAndSaveSectors() {
        Concept sector = new Concept();
        sector.setCode("BK0500");
        sector.setName("人工智能");
        when(conceptMapper.selectList(any())).thenReturn(List.of(sector));
        when(aktoolsClient.fetchConceptIndexDaily(eq("人工智能"), any(), any())).thenReturn("[{...}]");
        // 模拟 MyBatis-Plus insert 后自动回填 ID
        doAnswer(invocation -> {
            DataCollectionLog log = invocation.getArgument(0);
            log.setId(99L);
            return 1;
        }).when(logMapper).insert(any(DataCollectionLog.class));

        FetchResult result = handler.fetch(LocalDate.of(2026, 5, 20));
        assertThat(result.getType()).isEqualTo(FetchResult.Type.MULTI_SECTOR);
        assertThat(result.getSectorCount()).isEqualTo(1);
        assertThat(result.getCollectionLogId()).isEqualTo(99L);
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