package com.tradingdiary.service;

import com.tradingdiary.collection.model.CalendarDayVO;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private final TradeCalendarMapper tradeCalendarMapper;
    private final StockInfoMapper stockInfoMapper;
    private final StockDailyMapper stockDailyMapper;

    public CalendarService(TradeCalendarMapper tradeCalendarMapper,
                           StockInfoMapper stockInfoMapper,
                           StockDailyMapper stockDailyMapper) {
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.stockInfoMapper = stockInfoMapper;
        this.stockDailyMapper = stockDailyMapper;
    }

    public Map<String, Object> getMonthCalendar(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        List<TradeCalendar> tradingDays = tradeCalendarMapper.selectTradingDays(firstDay, lastDay);
        Set<LocalDate> tradeDateSet = tradingDays.stream()
                .map(TradeCalendar::getTradeDate)
                .collect(Collectors.toSet());

        Set<LocalDate> collectedSet = new HashSet<>();
        collectedSet.addAll(stockInfoMapper.selectDistinctSnapshotDates(firstDay, lastDay));
        collectedSet.addAll(stockDailyMapper.selectDistinctTradeDates(firstDay, lastDay));
        // stock_info 和 stock_daily 复用同一份 spot API 数据，并集即可

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
