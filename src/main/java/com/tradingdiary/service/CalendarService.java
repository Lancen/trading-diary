package com.tradingdiary.service;

import com.tradingdiary.collection.model.CalendarDayVO;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private final TradeCalendarMapper tradeCalendarMapper;
    private final StockDailyMapper stockDailyMapper;

    public CalendarService(TradeCalendarMapper tradeCalendarMapper, StockDailyMapper stockDailyMapper) {
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.stockDailyMapper = stockDailyMapper;
    }

    public Map<String, Object> getMonthCalendar(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        // 获取当月所有交易日
        List<TradeCalendar> tradingDays = tradeCalendarMapper.selectTradingDays(firstDay, lastDay);
        Set<LocalDate> tradeDateSet = tradingDays.stream()
                .map(TradeCalendar::getTradeDate)
                .collect(Collectors.toSet());

        // 获取当月有采集数据的日期（从 stock_daily 表）
        List<LocalDate> collectedDates = stockDailyMapper.selectDistinctTradeDates(firstDay, lastDay);
        Set<LocalDate> collectedSet = Set.copyOf(collectedDates);

        List<CalendarDayVO> days = new ArrayList<>();
        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            CalendarDayVO vo = new CalendarDayVO();
            vo.setDate(d);
            vo.setTradingDay(tradeDateSet.contains(d));
            vo.setHasData(collectedSet.contains(d));
            if (!tradeDateSet.contains(d)) {
                vo.setStatus("NON_TRADING");
            } else if (collectedSet.contains(d)) {
                vo.setStatus("COLLECTED");
            } else {
                vo.setStatus("MISSING");
            }
            days.add(vo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yearMonth", ym.toString());
        result.put("days", days);
        return result;
    }
}
