package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.mapper.SectorIndexDailyMapper;
import com.tradingdiary.service.collection.impl.SectorIndexDailyCleanseServiceImpl;
import com.tradingdiary.util.BatchSqlRunner;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorIndexDailyCleanseServiceImplTest {

    private SectorIndexDailyMapper sectorIndexDailyMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private SectorIndexDailyCleanseServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SectorIndexDaily.class);
    }

    @BeforeEach
    void setUp() {
        sectorIndexDailyMapper = mock(SectorIndexDailyMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new SectorIndexDailyCleanseServiceImpl(sectorIndexDailyMapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldCleanseIndustryWithValidJson() {
        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"开盘价\": \"1200.50\", \"最高价\": \"1220.00\", \"最低价\": \"1190.30\"," +
                " \"收盘价\": \"1210.80\", \"成交量\": \"50000000\", \"成交额\": \"6000000000\"}," +
                "{\"日期\": \"2026-05-19\", \"开盘价\": \"1180.00\", \"最高价\": \"1205.50\", \"最低价\": \"1175.20\"," +
                " \"收盘价\": \"1200.50\", \"成交量\": \"45000000\", \"成交额\": \"5400000000\"}" +
                "]";

        when(sectorIndexDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanse(rawJson, "INDUSTRY", "BK0475");

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldCleanseConceptWithValidJson() {
        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"开盘价\": \"800.50\", \"最高价\": \"815.00\", \"最低价\": \"795.30\"," +
                " \"收盘价\": \"810.80\", \"成交量\": \"30000000\", \"成交额\": \"2400000000\"}" +
                "]";

        when(sectorIndexDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(1);

        int result = service.cleanse(rawJson, "CONCEPT", "TK0801");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, "INDUSTRY", "BK0475");

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, "INDUSTRY", "BK0475"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析板块指数日线数据失败");
    }

    @Test
    void shouldComputeChangePctFromPreviousDay() {
        SectorIndexDaily prev = new SectorIndexDaily();
        prev.setSectorType("INDUSTRY");
        prev.setSectorCode("BK0475");
        prev.setTradeDate(LocalDate.of(2026, 5, 19));
        prev.setClose(new BigDecimal("1200.50"));

        when(sectorIndexDailyMapper.selectList(any())).thenReturn(List.of(prev));
        when(batchSqlRunner.batchInsert(any())).thenAnswer(invocation -> {
            List<SectorIndexDaily> list = invocation.getArgument(0);
            assertThat(list.get(0).getChangePct()).isEqualByComparingTo(new BigDecimal("0.86"));
            return list.size();
        });

        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"开盘价\": \"1200.50\", \"最高价\": \"1220.00\", \"最低价\": \"1190.30\"," +
                " \"收盘价\": \"1210.80\", \"成交量\": \"50000000\", \"成交额\": \"6000000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "INDUSTRY", "BK0475");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldSetChangePctNullWhenNoPreviousDay() {
        when(sectorIndexDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenAnswer(invocation -> {
            List<SectorIndexDaily> list = invocation.getArgument(0);
            assertThat(list.get(0).getChangePct()).isNull();
            return list.size();
        });

        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"开盘价\": \"1200.50\", \"最高价\": \"1220.00\", \"最低价\": \"1190.30\"," +
                " \"收盘价\": \"1210.80\", \"成交量\": \"50000000\", \"成交额\": \"6000000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "INDUSTRY", "BK0475");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldUpdateExistingRecords() {
        SectorIndexDaily existing = new SectorIndexDaily();
        existing.setId(1L);
        existing.setSectorType("INDUSTRY");
        existing.setSectorCode("BK0475");
        existing.setTradeDate(LocalDate.of(2026, 5, 20));
        existing.setClose(new BigDecimal("1200.00"));

        when(sectorIndexDailyMapper.selectList(any())).thenReturn(List.of(existing));
        when(batchSqlRunner.batchUpdate(any())).thenReturn(1);

        String rawJson = "[" +
                "{\"日期\": \"2026-05-20\", \"开盘价\": \"1200.50\", \"最高价\": \"1220.00\", \"最低价\": \"1190.30\"," +
                " \"收盘价\": \"1210.80\", \"成交量\": \"50000000\", \"成交额\": \"6000000000\"}" +
                "]";

        int result = service.cleanse(rawJson, "INDUSTRY", "BK0475");

        assertThat(result).isEqualTo(1);
        verify(batchSqlRunner).batchUpdate(any());
    }
}
