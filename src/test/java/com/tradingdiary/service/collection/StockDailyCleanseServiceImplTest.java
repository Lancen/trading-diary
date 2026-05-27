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
 * StockDailyCleanseServiceImpl 单元测试，验证股票日线数据清洗的 JSON 解析、去重和批量写入
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

    // 测试流程: Given 有效 AKTools JSON 且无已有记录, When 调用 cleanse, Then 解析 2 条并批量写入
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

    // 测试流程: Given 空 JSON 数组, When 调用 cleanse, Then 返回 0 且不触发批量写入
    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, TRADE_DATE);

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    // 测试流程: Given 无效 JSON 字符串, When 调用 cleanse, Then 抛出 RuntimeException 且消息包含"解析股票日线数据失败"
    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, TRADE_DATE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析股票日线数据失败");
    }

    // 测试流程: Given 有效 Tushare JSON 且无已有记录, When 调用 cleanseTushareDaily, Then 解析 1 条并批量写入
    @Test
    void shouldCleanseTushareDailySuccessfully() {
        String tushareJson = "{\"data\":{\"fields\":[\"ts_code\",\"trade_date\",\"open\",\"high\",\"low\",\"close\",\"vol\",\"amount\"]," +
                "\"items\":[[\"000001.SZ\",\"20260520\",\"12.50\",\"13.00\",\"12.30\",\"12.80\",\"523401\",\"6600000\"]]}}";

        when(stockDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(1);

        int result = service.cleanseTushareDaily(tushareJson);

        assertThat(result).isEqualTo(1);
    }

    // 测试流程: Given 有效 Tushare JSON, When 调用 parseTushareDaily, Then 解析出 1 条 StockDaily 且代码和日期正确
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
