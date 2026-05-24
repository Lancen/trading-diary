package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.service.collection.impl.MarginCleanseServiceImpl;
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

/**
 * 两融明细清洗服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class MarginCleanseServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 5, 20);

    private MarginDailyMapper marginDailyMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private MarginCleanseServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MarginDaily.class);
    }

    @BeforeEach
    void setUp() {
        marginDailyMapper = mock(MarginDailyMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new MarginCleanseServiceImpl(marginDailyMapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldCleanseWithValidSSEJson() {
        String rawJson = "[" +
                "{\"标的证券代码\": \"600000\", \"融资余额\": \"1000000\", \"融资买入额\": \"50000\", \"融资偿还额\": \"30000\"," +
                " \"融券余额\": \"200000\", \"融券卖出量\": \"1000\", \"融券偿还量\": \"500\", \"融券余量\": \"3000\"," +
                " \"融资融券余额\": \"1200000\"}," +
                "{\"标的证券代码\": \"600001\", \"融资余额\": \"2000000\", \"融资买入额\": \"80000\", \"融资偿还额\": \"60000\"," +
                " \"融券余额\": \"300000\", \"融券卖出量\": \"2000\", \"融券偿还量\": \"1000\", \"融券余量\": \"5000\"," +
                " \"融资融券余额\": \"2300000\"}" +
                "]";

        when(marginDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.cleanse(rawJson, "SSE", TRADE_DATE);

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldCleanseWithValidSZSEJson() {
        String rawJson = "[" +
                "{\"证券代码\": \"000001\", \"融资余额\": \"500000\", \"融资买入额\": \"20000\", \"融资偿还额\": \"10000\"," +
                " \"融券余额\": \"100000\", \"融券卖出量\": \"500\", \"融券偿还量\": \"200\", \"融券余量\": \"1500\"," +
                " \"融资融券余额\": \"600000\"}" +
                "]";

        when(marginDailyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(batchSqlRunner.batchInsert(any())).thenReturn(1);

        int result = service.cleanse(rawJson, "SZSE", TRADE_DATE);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroWhenEmptyData() {
        String rawJson = "[]";

        int result = service.cleanse(rawJson, "SSE", TRADE_DATE);

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
        verify(batchSqlRunner, never()).batchUpdate(any());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "not a json";

        assertThatThrownBy(() -> service.cleanse(invalidJson, "SSE", TRADE_DATE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析两融明细数据失败");
    }

    @Test
    void shouldComputeMarginChangeFromPreviousDay() {
        MarginDaily prev = new MarginDaily();
        prev.setStockCode("600000");
        prev.setTradeDate(TRADE_DATE.minusDays(1));
        prev.setExchange("SSE");
        prev.setMarginBalance(new BigDecimal("900000"));
        prev.setShortBalance(new BigDecimal("150000"));

        when(marginDailyMapper.selectList(any())).thenReturn(List.of(prev));
        when(batchSqlRunner.batchUpdate(any())).thenReturn(1);

        String rawJson = "[" +
                "{\"标的证券代码\": \"600000\", \"融资余额\": \"1000000\", \"融资买入额\": \"50000\", \"融资偿还额\": \"30000\"," +
                " \"融券余额\": \"200000\", \"融券卖出量\": \"1000\", \"融券偿还量\": \"500\", \"融券余量\": \"3000\"," +
                " \"融资融券余额\": \"1200000\"}" +
                "]";

        int result = service.cleanse(rawJson, "SSE", TRADE_DATE);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroWhenNonArrayJson() {
        String rawJson = "{\"key\": \"value\"}";

        int result = service.cleanse(rawJson, "SSE", TRADE_DATE);

        assertThat(result).isEqualTo(0);
    }
}
