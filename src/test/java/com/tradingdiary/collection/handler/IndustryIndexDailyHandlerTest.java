package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.IndustryMapper;
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
 * IndustryIndexDailyHandler 单元测试，验证行业指数日线采集的 fetchSectors 和 cleanse 流程
 */
@ExtendWith(MockitoExtension.class)
class IndustryIndexDailyHandlerTest {

    private AKToolsClient aktoolsClient;
    private SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private IndustryMapper industryMapper;
    private RawDataMapper rawDataMapper;
    private IndustryIndexDailyHandler handler;

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        sectorIndexDailyCleanseService = mock(SectorIndexDailyCleanseService.class);
        industryMapper = mock(IndustryMapper.class);
        rawDataMapper = mock(RawDataMapper.class);
        handler = new IndustryIndexDailyHandler(aktoolsClient, sectorIndexDailyCleanseService, industryMapper, rawDataMapper);
    }

    // 测试流程: 验证 dataType 返回 INDUSTRY_INDEX_DAILY
    @Test
    void shouldReturnCorrectDataType() {
        assertThat(handler.dataType()).isEqualTo("INDUSTRY_INDEX_DAILY");
    }

    // 测试流程: Given 行业指数不支持标准 fetch, When 调用 fetch, Then 抛出 UnsupportedOperationException
    @Test
    void shouldThrowOnStandardFetch() {
        assertThatThrownBy(() -> handler.fetch(LocalDate.now()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // 测试流程: Given industry 表为空, When 调用 fetchSectors, Then 抛出 IllegalStateException
    @Test
    void shouldThrowWhenIndustryTableEmpty() {
        when(industryMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> handler.fetchSectors(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20), 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // 测试流程: Given industry 表有板块数据且 client 返回行情, When 调用 fetchSectors, Then 返回 1 并保存 rawData
    @Test
    void shouldFetchAndSaveSectors() {
        Industry sector = new Industry();
        sector.setCode("BK0001");
        sector.setName("银行");
        when(industryMapper.selectList(any())).thenReturn(List.of(sector));
        when(aktoolsClient.fetchIndustryIndexDaily(eq("银行"), any(), any())).thenReturn("[{...}]");

        int count = handler.fetchSectors(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 20), 1L);
        assertThat(count).isEqualTo(1);
        verify(rawDataMapper).insert(any(RawData.class));
    }

    // 测试流程: Given rawData 中有行业指数数据, When 调用 cleanse, Then 以 INDUSTRY 类型委托清洗并返回 30
    @Test
    void shouldCleanseFromRawData() {
        RawData rd = new RawData();
        rd.setRawJson("[{...}]");
        rd.setSectorCode("BK0001");
        when(rawDataMapper.selectList(any())).thenReturn(List.of(rd));
        when(sectorIndexDailyCleanseService.cleanse("[{...}]", "INDUSTRY", "BK0001")).thenReturn(30);

        int count = handler.cleanse("[{...}]", LocalDate.of(2026, 5, 20));
        assertThat(count).isEqualTo(30);
    }
}