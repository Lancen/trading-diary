package com.tradingdiary.collection.orchestrator;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.BackfillRequest;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.collection.ConceptCleanseService;
import com.tradingdiary.service.collection.IndustryCleanseService;
import com.tradingdiary.service.collection.MarginCleanseService;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import com.tradingdiary.service.collection.StockInfoCleanseService;
import com.tradingdiary.service.collection.TradeCalendarService;
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

@ExtendWith(MockitoExtension.class)
class CollectionOrchestratorTest {

    private TradeCalendarMapper tradeCalendarMapper;
    private DataCollectionLogMapper dataCollectionLogMapper;
    private CollectionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        tradeCalendarMapper = mock(TradeCalendarMapper.class);
        dataCollectionLogMapper = mock(DataCollectionLogMapper.class);
        AKToolsClient aktoolsClient = mock(AKToolsClient.class);
        RawDataMapper rawDataMapper = mock(RawDataMapper.class);
        StockInfoCleanseService stockInfoCleanseService = mock(StockInfoCleanseService.class);
        StockDailyCleanseService stockDailyCleanseService = mock(StockDailyCleanseService.class);
        IndustryCleanseService industryCleanseService = mock(IndustryCleanseService.class);
        ConceptCleanseService conceptCleanseService = mock(ConceptCleanseService.class);
        MarginCleanseService marginCleanseService = mock(MarginCleanseService.class);
        TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);
        IndustryMapper industryMapper = mock(IndustryMapper.class);
        ConceptMapper conceptMapper = mock(ConceptMapper.class);
        StockInfoMapper stockInfoMapper = mock(StockInfoMapper.class);

        CollectionOrchestrator real = new CollectionOrchestrator(
                aktoolsClient, dataCollectionLogMapper, rawDataMapper,
                stockInfoCleanseService, stockDailyCleanseService,
                industryCleanseService, conceptCleanseService,
                marginCleanseService, tradeCalendarService,
                tradeCalendarMapper, industryMapper, conceptMapper,
                stockInfoMapper
        );
        orchestrator = spy(real);
    }

    @Test
    void shouldSkipCompleteWeekWhenAllDatesHaveSuccessLogs() {
        // Given: 5 trading days (Mon-Fri) with all SUCCESS logs
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

        // When
        String result = orchestrator.backfillMarginByWeek(request);

        // Then
        assertThat(result).contains("skipped");
        verify(orchestrator, never()).orchestrate(any(), any());
    }

    @Test
    void shouldOrchestrateAllDatesWhenNoLogsExist() {
        // Given: 3 trading days with no prior logs
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

        // When
        String result = orchestrator.backfillMarginByWeek(request);

        // Then
        assertThat(result).contains("Backfill complete");
        verify(orchestrator, times(3)).orchestrate(eq("MARGIN_DAILY_SSE"), any());
    }

    @Test
    void shouldHandleEmptyTradingDays() {
        // Given: no trading days in range
        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(Collections.emptyList());

        BackfillRequest request = new BackfillRequest();
        request.setDataType("MARGIN_DAILY_SSE");
        request.setExchange("SSE");
        request.setStartDate(LocalDate.of(2026, 5, 9));
        request.setEndDate(LocalDate.of(2026, 5, 10));

        // When
        String result = orchestrator.backfillMarginByWeek(request);

        // Then
        assertThat(result).contains("No trading days");
        verify(orchestrator, never()).orchestrate(any(), any());
    }

    @Test
    void shouldOrchestrateOnlyMissingDatesInPartialWeek() {
        // Given: 3 trading days, only day 1 has SUCCESS logs
        List<TradeCalendar> tradingDays = Arrays.asList(
                buildTradingDay(LocalDate.of(2026, 5, 11)),
                buildTradingDay(LocalDate.of(2026, 5, 12)),
                buildTradingDay(LocalDate.of(2026, 5, 13))
        );

        when(tradeCalendarMapper.selectTradingDays(any(), any())).thenReturn(tradingDays);

        // Day 1: both SUCCESS
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(LocalDate.of(2026, 5, 11))))
                .thenReturn(buildSuccessLog("SUCCESS"));
        when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(LocalDate.of(2026, 5, 11))))
                .thenReturn(buildSuccessLog("SUCCESS"));

        // Days 2-3: no logs
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

        // When
        String result = orchestrator.backfillMarginByWeek(request);

        // Then
        assertThat(result).contains("Backfill complete");
        // Only 2 dates need orchestration (day 1 was already complete)
        verify(orchestrator, times(2)).orchestrate(eq("MARGIN_DAILY_SSE"), any());
    }

    @Test
    void shouldGroupMultipleWeeksCorrectly() {
        // Given: 2 weeks worth of trading days
        // Week 1 (May 4-8): all SUCCESS (should be skipped)
        // Week 2 (May 11-13): no logs (should be orchestrated)
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

        // Week 1: all SUCCESS
        for (int i = 4; i <= 8; i++) {
            LocalDate date = LocalDate.of(2026, 5, i);
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("FETCH"), eq(date)))
                    .thenReturn(buildSuccessLog("SUCCESS"));
            when(dataCollectionLogMapper.selectLatestByDataTypeAndJobTypeAndTradeDate(
                    eq("MARGIN_DAILY_SSE"), eq("CLEANSE"), eq(date)))
                    .thenReturn(buildSuccessLog("SUCCESS"));
        }

        // Week 2: no logs
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

        // When
        String result = orchestrator.backfillMarginByWeek(request);

        // Then
        assertThat(result).contains("Backfill complete");
        assertThat(result).contains("skipped");
        // Only week 2 dates (3 dates) need orchestration
        verify(orchestrator, times(3)).orchestrate(eq("MARGIN_DAILY_SSE"), any());
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
