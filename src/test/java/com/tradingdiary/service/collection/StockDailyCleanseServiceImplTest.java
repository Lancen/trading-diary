package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.service.collection.impl.StockDailyCleanseServiceImpl;
import com.tradingdiary.util.BatchSqlRunner;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 股票日线清洗服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class StockDailyCleanseServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 5, 20);

    private StockDailyMapper stockDailyMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private StockDailyCleanseServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, StockDaily.class);
    }

    @BeforeEach
    void setUp() {
        stockDailyMapper = mock(StockDailyMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new StockDailyCleanseServiceImpl(stockDailyMapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldCleanseWithValidJson() {
        String rawJson = "[" +
                "{\"代码\": \"000001\", \"今开\": \"12.50\", \"最高\": \"13.00\", \"最低\": \"12.30\"," +
                " \"最新价\": \"12.80\", \"成交量\": \"52340100\", \"成交额\": \"660000000\"}," +
                "{\"代码\": \"000002\", \"今开\": \"8.10\", \"最高\": \"8.50\", \"最低\": \"8.00\"," +
                " \"最新价\": \"8.30\", \"成交量\": \"30120000\", \"成交额\": \"250000000\"}" +
                "]";

        when(stockDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanse(rawJson, TRADE_DATE);

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, TRADE_DATE);

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, TRADE_DATE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析股票日线数据失败");
    }

    @Test
    void shouldCleanseHistBatchSuccessfully() {
        String histJson1 = "[" +
                "{\"date\": \"2026-05-18\", \"open\": \"12.00\", \"high\": \"12.50\", \"low\": \"11.80\"," +
                " \"close\": \"12.30\", \"volume\": \"40000000\", \"amount\": \"500000000\"}" +
                "]";
        String histJson2 = "[" +
                "{\"date\": \"2026-05-19\", \"open\": \"12.40\", \"high\": \"12.80\", \"low\": \"12.20\"," +
                " \"close\": \"12.60\", \"volume\": \"45000000\", \"amount\": \"560000000\"}" +
                "]";

        when(stockDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanseHistBatch(
                List.of(histJson1, histJson2),
                List.of("000001", "000002"));

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldCleanseHistBatchWithEmptyData() {
        String emptyJson = "[]";

        when(stockDailyMapper.selectList(any())).thenReturn(Collections.emptyList());

        int result = service.cleanseHistBatch(List.of(emptyJson), List.of("000001"));

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    @Test
    void shouldCleanseTushareDailySuccessfully() {
        String tushareJson = "{\"data\":{\"fields\":[\"ts_code\",\"trade_date\",\"open\",\"high\",\"low\",\"close\",\"vol\",\"amount\"]," +
                "\"items\":[[\"000001.SZ\",\"20260520\",\"12.50\",\"13.00\",\"12.30\",\"12.80\",\"523401\",\"6600000\"]]}}";

        when(stockDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(1);

        int result = service.cleanseTushareDaily(tushareJson);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldParseTushareDailyCorrectly() {
        String tushareJson = "{\"data\":{\"fields\":[\"ts_code\",\"trade_date\",\"open\",\"high\",\"low\",\"close\",\"vol\",\"amount\"]," +
                "\"items\":[[\"600000.SH\",\"20260520\",\"8.10\",\"8.50\",\"8.00\",\"8.30\",\"301200\",\"2500000\"]]}}";

        List<StockDaily> result = service.parseTushareDaily(tushareJson);

        assertThat(result).hasSize(1);
        StockDaily daily = result.get(0);
        assertThat(daily.getStockCode()).isEqualTo("600000");
        assertThat(daily.getTradeDate()).isEqualTo(TRADE_DATE);
    }
}
