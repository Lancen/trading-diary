package com.tradingdiary.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tradingdiary.collection.model.CalendarDayVO;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.impl.CalendarServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 交易日历服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class CalendarServiceImplTest {

    private TradeCalendarMapper tradeCalendarMapper;
    private StockInfoMapper stockInfoMapper;
    private StockDailyMapper stockDailyMapper;
    private MarginDailyMapper marginDailyMapper;
    private MarginMacroMapper marginMacroMapper;
    private CalendarServiceImpl service;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, TradeCalendar.class);
        TableInfoHelper.initTableInfo(assistant, StockInfo.class);
        TableInfoHelper.initTableInfo(assistant, StockDaily.class);
        TableInfoHelper.initTableInfo(assistant, MarginDaily.class);
    }

    @BeforeEach
    void setUp() {
        tradeCalendarMapper = mock(TradeCalendarMapper.class);
        stockInfoMapper = mock(StockInfoMapper.class);
        stockDailyMapper = mock(StockDailyMapper.class);
        marginDailyMapper = mock(MarginDailyMapper.class);
        marginMacroMapper = mock(MarginMacroMapper.class);
        service = new CalendarServiceImpl(
                tradeCalendarMapper, stockInfoMapper, stockDailyMapper,
                marginDailyMapper, marginMacroMapper);
    }

    @Test
    void shouldReturnCalendarWithTradingDays() {
        LocalDate firstDay = LocalDate.of(2026, 5, 1);
        LocalDate lastDay = LocalDate.of(2026, 5, 31);

        TradeCalendar tc1 = new TradeCalendar();
        tc1.setTradeDate(LocalDate.of(2026, 5, 1));
        tc1.setIsTradingDay(1);

        when(tradeCalendarMapper.selectTradingDays(firstDay, lastDay))
                .thenReturn(List.of(tc1));
        when(stockInfoMapper.selectDistinctSnapshotDates(firstDay, lastDay))
                .thenReturn(List.of(LocalDate.of(2026, 5, 1)));
        when(stockDailyMapper.selectDistinctTradeDates(firstDay, lastDay))
                .thenReturn(List.of());

        Map<String, Object> result = service.getMonthCalendar(2026, 5, null);

        assertThat(result.get("yearMonth")).isEqualTo("2026-05");
        @SuppressWarnings("unchecked")
        List<CalendarDayVO> days = (List<CalendarDayVO>) result.get("days");
        assertThat(days).hasSize(31);

        CalendarDayVO may1 = days.get(0);
        assertThat(may1.getDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(may1.isTradingDay()).isTrue();
        assertThat(may1.isHasData()).isTrue();
        assertThat(may1.getStatus()).isEqualTo("COLLECTED");

        CalendarDayVO may2 = days.get(1);
        assertThat(may2.isTradingDay()).isFalse();
        assertThat(may2.getStatus()).isEqualTo("NON_TRADING");
    }

    @Test
    void shouldReturnMissingStatusForUncollectedTradingDay() {
        LocalDate firstDay = LocalDate.of(2026, 5, 1);
        LocalDate lastDay = LocalDate.of(2026, 5, 31);

        TradeCalendar tc1 = new TradeCalendar();
        tc1.setTradeDate(LocalDate.of(2026, 5, 1));
        tc1.setIsTradingDay(1);

        when(tradeCalendarMapper.selectTradingDays(firstDay, lastDay))
                .thenReturn(List.of(tc1));
        when(stockInfoMapper.selectDistinctSnapshotDates(firstDay, lastDay))
                .thenReturn(List.of());
        when(stockDailyMapper.selectDistinctTradeDates(firstDay, lastDay))
                .thenReturn(List.of());

        Map<String, Object> result = service.getMonthCalendar(2026, 5, null);

        @SuppressWarnings("unchecked")
        List<CalendarDayVO> days = (List<CalendarDayVO>) result.get("days");
        CalendarDayVO may1 = days.get(0);
        assertThat(may1.isTradingDay()).isTrue();
        assertThat(may1.isHasData()).isFalse();
        assertThat(may1.getStatus()).isEqualTo("MISSING");
    }

    @Test
    void shouldUseMarginDailyForMarginDataType() {
        LocalDate firstDay = LocalDate.of(2026, 5, 1);
        LocalDate lastDay = LocalDate.of(2026, 5, 31);

        TradeCalendar tc1 = new TradeCalendar();
        tc1.setTradeDate(LocalDate.of(2026, 5, 1));
        tc1.setIsTradingDay(1);

        when(tradeCalendarMapper.selectTradingDays(firstDay, lastDay))
                .thenReturn(List.of(tc1));
        when(marginDailyMapper.selectDistinctTradeDates(firstDay, lastDay, "SSE"))
                .thenReturn(List.of(LocalDate.of(2026, 5, 1)));

        Map<String, Object> result = service.getMonthCalendar(2026, 5, "MARGIN_DAILY_SSE");

        @SuppressWarnings("unchecked")
        List<CalendarDayVO> days = (List<CalendarDayVO>) result.get("days");
        CalendarDayVO may1 = days.get(0);
        assertThat(may1.isHasData()).isTrue();
        assertThat(may1.getStatus()).isEqualTo("COLLECTED");
    }
}
