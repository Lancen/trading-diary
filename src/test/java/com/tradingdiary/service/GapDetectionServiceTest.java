package com.tradingdiary.service;

import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GapDetectionServiceTest {

    @Mock
    private TradeCalendarMapper tradeCalendarMapper;

    @Mock
    private MarginDailyMapper marginDailyMapper;

    @InjectMocks
    private GapDetectionService gapDetectionService;

    @Test
    void shouldDetectGapsWhenSomeTradingDaysMissing() {
        LocalDate start = LocalDate.of(2026, 5, 11);
        LocalDate end = LocalDate.of(2026, 5, 15);

        List<TradeCalendar> tradingDays = new ArrayList<>();
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 11)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 12)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 13)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 14)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 15)));

        List<LocalDate> collectedDates = List.of(
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 13),
                LocalDate.of(2026, 5, 15)
        );

        when(tradeCalendarMapper.selectTradingDays(eq(start), eq(end))).thenReturn(tradingDays);
        when(marginDailyMapper.selectDistinctTradeDates(start, end, "SSE"))
                .thenReturn(collectedDates);

        GapReportVO report = gapDetectionService.getGaps(start, end, "SSE");

        assertThat(report.getWeeks()).hasSize(1);
        assertThat(report.getTotalWeeks()).isEqualTo(1);
        assertThat(report.getCompleteWeeks()).isEqualTo(0);
        assertThat(report.getPartialWeeks()).isEqualTo(1);
        assertThat(report.getMissingWeeks()).isEqualTo(0);

        GapReportVO.WeekGap week = report.getWeeks().get(0);
        assertThat(week.getExpectedDays()).isEqualTo(5);
        assertThat(week.getCollectedDays()).isEqualTo(3);
        assertThat(week.getMissingDates()).containsExactly("2026-05-12", "2026-05-14");
        assertThat(week.getStatus()).isEqualTo("PARTIAL");
    }

    @Test
    void shouldReturnCompleteWhenNoGaps() {
        LocalDate start = LocalDate.of(2026, 5, 11);
        LocalDate end = LocalDate.of(2026, 5, 13);

        List<TradeCalendar> tradingDays = new ArrayList<>();
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 11)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 12)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 13)));

        List<LocalDate> collectedDates = List.of(
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 13)
        );

        when(tradeCalendarMapper.selectTradingDays(eq(start), eq(end))).thenReturn(tradingDays);
        when(marginDailyMapper.selectDistinctTradeDates(start, end, "SZSE"))
                .thenReturn(collectedDates);

        GapReportVO report = gapDetectionService.getGaps(start, end, "SZSE");

        assertThat(report.getCompleteWeeks()).isEqualTo(1);
        assertThat(report.getPartialWeeks()).isEqualTo(0);

        GapReportVO.WeekGap week = report.getWeeks().get(0);
        assertThat(week.getExpectedDays()).isEqualTo(3);
        assertThat(week.getCollectedDays()).isEqualTo(3);
        assertThat(week.getMissingDates()).isEmpty();
        assertThat(week.getStatus()).isEqualTo("COMPLETE");
    }

    @Test
    void shouldReturnMissingWhenAllTradingDaysMissing() {
        LocalDate start = LocalDate.of(2026, 5, 11);
        LocalDate end = LocalDate.of(2026, 5, 12);

        List<TradeCalendar> tradingDays = new ArrayList<>();
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 11)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 12)));

        when(tradeCalendarMapper.selectTradingDays(eq(start), eq(end))).thenReturn(tradingDays);
        when(marginDailyMapper.selectDistinctTradeDates(start, end, "SSE"))
                .thenReturn(List.of());

        GapReportVO report = gapDetectionService.getGaps(start, end, "SSE");

        assertThat(report.getMissingWeeks()).isEqualTo(1);
        assertThat(report.getCompleteWeeks()).isEqualTo(0);

        GapReportVO.WeekGap week = report.getWeeks().get(0);
        assertThat(week.getExpectedDays()).isEqualTo(2);
        assertThat(week.getCollectedDays()).isEqualTo(0);
        assertThat(week.getMissingDates()).hasSize(2);
        assertThat(week.getStatus()).isEqualTo("MISSING");
    }

    @Test
    void shouldHandleEmptyTradingCalendar() {
        LocalDate start = LocalDate.of(2026, 5, 11);
        LocalDate end = LocalDate.of(2026, 5, 15);

        when(tradeCalendarMapper.selectTradingDays(eq(start), eq(end))).thenReturn(List.of());

        GapReportVO report = gapDetectionService.getGaps(start, end, "SSE");

        assertThat(report.getWeeks()).isEmpty();
        assertThat(report.getTotalWeeks()).isEqualTo(0);
    }

    @Test
    void shouldHandleMultiWeekRange() {
        LocalDate start = LocalDate.of(2026, 5, 4);
        LocalDate end = LocalDate.of(2026, 5, 15);

        List<TradeCalendar> tradingDays = new ArrayList<>();
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 4)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 5)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 6)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 7)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 8)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 11)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 12)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 13)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 14)));
        tradingDays.add(buildCalendar(LocalDate.of(2026, 5, 15)));

        List<LocalDate> collectedDates = List.of(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 5),
                LocalDate.of(2026, 5, 6),
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 8),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 15)
        );

        when(tradeCalendarMapper.selectTradingDays(eq(start), eq(end))).thenReturn(tradingDays);
        when(marginDailyMapper.selectDistinctTradeDates(start, end, "SZSE"))
                .thenReturn(collectedDates);

        GapReportVO report = gapDetectionService.getGaps(start, end, "SZSE");

        assertThat(report.getWeeks()).hasSize(2);
        assertThat(report.getTotalWeeks()).isEqualTo(2);
        assertThat(report.getCompleteWeeks()).isEqualTo(1);
        assertThat(report.getPartialWeeks()).isEqualTo(1);
        assertThat(report.getMissingWeeks()).isEqualTo(0);

        GapReportVO.WeekGap week1 = report.getWeeks().get(0);
        assertThat(week1.getStatus()).isEqualTo("COMPLETE");
        assertThat(week1.getExpectedDays()).isEqualTo(5);
        assertThat(week1.getCollectedDays()).isEqualTo(5);

        GapReportVO.WeekGap week2 = report.getWeeks().get(1);
        assertThat(week2.getStatus()).isEqualTo("PARTIAL");
        assertThat(week2.getExpectedDays()).isEqualTo(5);
        assertThat(week2.getCollectedDays()).isEqualTo(2);
        assertThat(week2.getMissingDates()).containsExactly("2026-05-12", "2026-05-13", "2026-05-14");
    }

    private TradeCalendar buildCalendar(LocalDate date) {
        TradeCalendar tc = new TradeCalendar();
        tc.setTradeDate(date);
        tc.setIsTradingDay(1);
        return tc;
    }
}
