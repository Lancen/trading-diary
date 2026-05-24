package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.collection.impl.TradeCalendarServiceImpl;
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
 * 交易日历同步服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class TradeCalendarServiceImplTest {

    private AKToolsClient aktoolsClient;
    private TradeCalendarMapper tradeCalendarMapper;
    private BatchSqlRunner batchSqlRunner;
    private ObjectMapper objectMapper;
    private TradeCalendarServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, TradeCalendar.class);
    }

    @BeforeEach
    void setUp() {
        aktoolsClient = mock(AKToolsClient.class);
        tradeCalendarMapper = mock(TradeCalendarMapper.class);
        batchSqlRunner = mock(BatchSqlRunner.class);
        objectMapper = new ObjectMapper();
        service = new TradeCalendarServiceImpl(aktoolsClient, tradeCalendarMapper, batchSqlRunner, objectMapper);
    }

    @Test
    void shouldSyncTradeCalendarSuccessfully() {
        String responseJson = "[" +
                "{\"trade_date\": \"2026-05-18\"}," +
                "{\"trade_date\": \"2026-05-19\"}," +
                "{\"trade_date\": \"2026-05-20\"}" +
                "]";

        when(aktoolsClient.fetchTradeCalendar()).thenReturn(responseJson);
        when(tradeCalendarMapper.selectList(any())).thenReturn(
                List.of(buildTradeCalendar(LocalDate.of(2026, 5, 18))));
        when(batchSqlRunner.batchInsert(any())).thenReturn(2);

        int result = service.syncTradeCalendar();

        assertThat(result).isEqualTo(2);
        verify(batchSqlRunner).batchInsert(any());
    }

    @Test
    void shouldReturnZeroWhenEmptyResponse() {
        String responseJson = "[]";

        when(aktoolsClient.fetchTradeCalendar()).thenReturn(responseJson);

        int result = service.syncTradeCalendar();

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    @Test
    void shouldSkipExistingDates() {
        String responseJson = "[" +
                "{\"trade_date\": \"2026-05-18\"}," +
                "{\"trade_date\": \"2026-05-19\"}" +
                "]";

        when(aktoolsClient.fetchTradeCalendar()).thenReturn(responseJson);
        when(tradeCalendarMapper.selectList(any())).thenReturn(
                List.of(buildTradeCalendar(LocalDate.of(2026, 5, 18)),
                        buildTradeCalendar(LocalDate.of(2026, 5, 19))));

        int result = service.syncTradeCalendar();

        assertThat(result).isEqualTo(0);
        verify(batchSqlRunner, never()).batchInsert(any());
    }

    @Test
    void shouldThrowOnInvalidResponse() {
        String invalidJson = "not a json";

        when(aktoolsClient.fetchTradeCalendar()).thenReturn(invalidJson);

        assertThatThrownBy(() -> service.syncTradeCalendar())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("解析交易日历数据失败");
    }

    private TradeCalendar buildTradeCalendar(LocalDate date) {
        TradeCalendar tc = new TradeCalendar();
        tc.setTradeDate(date);
        tc.setIsTradingDay(1);
        return tc;
    }
}
