package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.handler.DataTypeHandler;
import com.tradingdiary.collection.handler.StockDailyTushareHandler;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CollectionOrchestrator 单元测试，验证 handler registry 模式下的回补编排和幂等跳过逻辑
 */
@ExtendWith(MockitoExtension.class)
class CollectionOrchestratorTest {

    private TradeCalendarMapper tradeCalendarMapper;
    private DataCollectionLogMapper dataCollectionLogMapper;
    private CollectionOrchestrator orchestrator;
    private DataTypeHandler tushareHandler;

    @BeforeEach
    void setUp() {
        tradeCalendarMapper = mock(TradeCalendarMapper.class);
        dataCollectionLogMapper = mock(DataCollectionLogMapper.class);
        RawDataMapper rawDataMapper = mock(RawDataMapper.class);

        tushareHandler = mock(StockDailyTushareHandler.class);
        when(tushareHandler.dataType()).thenReturn("STOCK_DAILY_TUSHARE");

        // 注册 margin handler 用于回补测试
        DataTypeHandler marginHandler = mock(DataTypeHandler.class);
        when(marginHandler.dataType()).thenReturn("MARGIN_DAILY_SSE");

        List<DataTypeHandler> handlers = List.of(tushareHandler, marginHandler);

        CollectionOrchestrator real = new CollectionOrchestrator(
                handlers, dataCollectionLogMapper, rawDataMapper, tradeCalendarMapper);
        orchestrator = spy(real);
    }

    // 测试流程: Given 所有交易日均有成功日志, When 回补整周, Then 跳过且不调用 orchestrate
    @Test
    void shouldSkipCompleteWeekWhenAllDatesHaveSuccessLogs() {
        List<TradeCalendar> tradingDays = Arrays.asList(
                buildTradingDay(LocalDate.of(2026, 5, 4)),
                buildTradingDay(LocalDate.of(2026, 5, 5)),
                buildTradingDay(LocalDate.of(2026, 5, 6)),
                buildTradingDay(LocalDate.of(2026, 5, 7)),
                buildTradingDay(LocalDate.of(2026, 5, 8))
        );

        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(tradingDays);

        for (TradeCalendar day : tradingDays) {
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(day.getTradeDate())))
                    .thenReturn(buildSuccessLog("SUCCESS"));
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(day.getTradeDate())))
                    .thenReturn(buildSuccessLog("SUCCESS"));
        }

        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 8));

        String result = orchestrator.backfillMarginByWeek(request);

        assertThat(result).contains("已跳过");
        verify(orchestrator, never()).orchestrate(any(), any());
    }

    // 测试流程: Given 所有交易日均无日志, When 回补, Then 对每个交易日调用 orchestrate
    @Test
    void shouldOrchestrateAllDatesWhenNoLogsExist() {
        List<TradeCalendar> tradingDays = Arrays.asList(
                buildTradingDay(LocalDate.of(2026, 5, 11)),
                buildTradingDay(LocalDate.of(2026, 5, 12)),
                buildTradingDay(LocalDate.of(2026, 5, 13))
        );

        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(tradingDays);

        for (TradeCalendar day : tradingDays) {
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(day.getTradeDate())))
                    .thenReturn(null);
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(day.getTradeDate())))
                    .thenReturn(null);
        }

        doReturn("success").when(orchestrator).orchestrate(any(), any());

        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 11));
        request.setEndDate(LocalDate.of(2026, 5, 13));

        String result = orchestrator.backfillMarginByWeek(request);

        assertThat(result).contains("补采完成");
        verify(orchestrator, times(3)).orchestrate(eq("MARGIN_DAILY_SSE"), any());
    }

    // 测试流程: Given 日期范围内无交易日, When 回补, Then 返回"此范围无交易日"且不调用 orchestrate
    @Test
    void shouldHandleEmptyTradingDays() {
        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(Collections.emptyList());

        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 9));
        request.setEndDate(LocalDate.of(2026, 5, 10));

        String result = orchestrator.backfillMarginByWeek(request);

        assertThat(result).contains("此范围无交易日");
        verify(orchestrator, never()).orchestrate(any(), any());
    }

    // 测试流程: Given 部分交易日已完成, When 回补部分周, Then 仅对缺失日期调用 orchestrate（2次）
    @Test
    void shouldOrchestrateOnlyMissingDatesInPartialWeek() {
        List<TradeCalendar> tradingDays = Arrays.asList(
                buildTradingDay(LocalDate.of(2026, 5, 11)),
                buildTradingDay(LocalDate.of(2026, 5, 12)),
                buildTradingDay(LocalDate.of(2026, 5, 13))
        );

        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(tradingDays);

        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(LocalDate.of(2026, 5, 11))))
                .thenReturn(buildSuccessLog("SUCCESS"));
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(LocalDate.of(2026, 5, 11))))
                .thenReturn(buildSuccessLog("SUCCESS"));

        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(LocalDate.of(2026, 5, 12))))
                .thenReturn(null);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(LocalDate.of(2026, 5, 12))))
                .thenReturn(null);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(LocalDate.of(2026, 5, 13))))
                .thenReturn(null);
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(LocalDate.of(2026, 5, 13))))
                .thenReturn(null);

        doReturn("success").when(orchestrator).orchestrate(any(), any());

        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 11));
        request.setEndDate(LocalDate.of(2026, 5, 13));

        String result = orchestrator.backfillMarginByWeek(request);

        assertThat(result).contains("补采完成");
        verify(orchestrator, times(2)).orchestrate(eq("MARGIN_DAILY_SSE"), any());
    }

    // 测试流程: Given 跨两周数据（第一周已完成、第二周缺失）, When 回补, Then 跳过第一周、补采第二周（3次 orchestrate）
    @Test
    void shouldGroupMultipleWeeksCorrectly() {
        List<TradeCalendar> tradingDays = Arrays.asList(
                buildTradingDay(LocalDate.of(2026, 5, 4)),
                buildTradingDay(LocalDate.of(2026, 5, 5)),
                buildTradingDay(LocalDate.of(2026, 5, 6)),
                buildTradingDay(LocalDate.of(2026, 5, 7)),
                buildTradingDay(LocalDate.of(2026, 5, 8)),
                buildTradingDay(LocalDate.of(2026, 5, 11)),
                buildTradingDay(LocalDate.of(2026, 5, 12)),
                buildTradingDay(LocalDate.of(2026, 5, 13))
        );

        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(tradingDays);

        for (int i = 4; i <= 8; i++) {
            LocalDate date = LocalDate.of(2026, 5, i);
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(date)))
                    .thenReturn(buildSuccessLog("SUCCESS"));
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(date)))
                    .thenReturn(buildSuccessLog("SUCCESS"));
        }

        for (int i = 11; i <= 13; i++) {
            LocalDate date = LocalDate.of(2026, 5, i);
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(date)))
                    .thenReturn(null);
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(date)))
                    .thenReturn(null);
        }

        doReturn("success").when(orchestrator).orchestrate(any(), any());

        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 4));
        request.setEndDate(LocalDate.of(2026, 5, 13));

        String result = orchestrator.backfillMarginByWeek(request);

        assertThat(result).contains("补采完成");
        assertThat(result).contains("已跳过");
        verify(orchestrator, times(3)).orchestrate(eq("MARGIN_DAILY_SSE"), any());
    }

    // 测试流程: Given tushareHandler 返回 FetchResult.single, When 回补 5 天, Then 总计 25000 条且 0 天失败
    @Test
    void shouldBackfillStockDailyWithTushareHandler() {
        for (int i = 1; i <= 5; i++) {
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("STOCK_DAILY_TUSHARE"), eq("FETCH"), eq(LocalDate.of(2026, 5, i))))
                    .thenReturn(null);
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("STOCK_DAILY_TUSHARE"), eq("CLEANSE"), eq(LocalDate.of(2026, 5, i))))
                    .thenReturn(null);
        }
        doReturn("执行成功").when(orchestrator).orchestrate(any(), any());

        String result = orchestrator.backfillStockDaily(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));

        assertThat(result).contains("日线补采完成");
        assertThat(result).contains("5 个日期已处理");
        verify(orchestrator, times(5)).orchestrate(eq("STOCK_DAILY_TUSHARE"), any());
    }

    // 测试流程: Given 结束日期早于开始日期, When 回补, Then 返回 0 个日期已处理
    @Test
    void shouldHandleEmptyDateRange() {
        String result = orchestrator.backfillStockDaily(
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 1));

        assertThat(result).contains("0 个日期已处理");
        verify(orchestrator, never()).orchestrate(any(), any());
    }

    // 测试流程: Given 部分日期 orchestrate 失败, When 回补 3 天, Then 继续执行后续天、1 天失败
    @Test
    void shouldContinueBackfillAfterSingleDateFailure() {
        doReturn(false).when(orchestrator).isDateComplete(any(), any());
        doReturn("执行成功").when(orchestrator).orchestrate(eq("STOCK_DAILY_TUSHARE"), eq(LocalDate.of(2026, 5, 1)));
        doReturn("采集失败").when(orchestrator).orchestrate(eq("STOCK_DAILY_TUSHARE"), eq(LocalDate.of(2026, 5, 2)));
        doReturn("执行成功").when(orchestrator).orchestrate(eq("STOCK_DAILY_TUSHARE"), eq(LocalDate.of(2026, 5, 3)));

        String result = orchestrator.backfillStockDaily(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

        assertThat(result).contains("1 个日期失败");
        verify(orchestrator, times(3)).orchestrate(eq("STOCK_DAILY_TUSHARE"), any());
    }

    private TradeCalendar buildTradingDay(LocalDate date) {
        TradeCalendar tc = new TradeCalendar();
        tc.setId(1L);
        tc.setTradeDate(date);
        tc.setIsTradingDay(1);
        return tc;
    }

    private DataCollectionLog buildSuccessLog(String status) {
        DataCollectionLog log = new DataCollectionLog();
        log.setId(1L);
        log.setStatus(status);
        return log;
    }
}