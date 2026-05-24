package com.tradingdiary.service.impl;

import com.tradingdiary.collection.model.CalendarDayVO;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.CalendarService;
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
public class CalendarServiceImpl implements CalendarService {

    private final TradeCalendarMapper tradeCalendarMapper;
    private final StockInfoMapper stockInfoMapper;
    private final StockDailyMapper stockDailyMapper;
    private final MarginDailyMapper marginDailyMapper;
    private final MarginMacroMapper marginMacroMapper;

    public CalendarServiceImpl(TradeCalendarMapper tradeCalendarMapper,
                               StockInfoMapper stockInfoMapper,
                               StockDailyMapper stockDailyMapper,
                               MarginDailyMapper marginDailyMapper,
                               MarginMacroMapper marginMacroMapper) {
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.stockInfoMapper = stockInfoMapper;
        this.stockDailyMapper = stockDailyMapper;
        this.marginDailyMapper = marginDailyMapper;
        this.marginMacroMapper = marginMacroMapper;
    }

    @Override
    public Map<String, Object> getMonthCalendar(int year, int month, String dataType) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        List<TradeCalendar> tradingDays = tradeCalendarMapper.selectTradingDays(firstDay, lastDay);
        Set<LocalDate> tradeDateSet = tradingDays.stream()
                .map(TradeCalendar::getTradeDate)
                .collect(Collectors.toSet());

        Set<LocalDate> collectedSet = resolveDataSource(dataType, firstDay, lastDay);

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

    private Set<LocalDate> resolveDataSource(String dataType, LocalDate firstDay, LocalDate lastDay) {
        Set<LocalDate> dates = new HashSet<>();
        if (dataType == null) dataType = "";

        switch (dataType) {
            case "MARGIN_DAILY_SSE":
                dates.addAll(marginDailyMapper.selectDistinctTradeDates(firstDay, lastDay, "SSE"));
                break;
            case "MARGIN_DAILY_SZSE":
                dates.addAll(marginDailyMapper.selectDistinctTradeDates(firstDay, lastDay, "SZSE"));
                break;
            case "MARGIN_MACRO_SSE":
                dates.addAll(marginMacroMapper.selectDistinctTradeDates(firstDay, lastDay, "SSE"));
                break;
            case "MARGIN_MACRO_SZSE":
                dates.addAll(marginMacroMapper.selectDistinctTradeDates(firstDay, lastDay, "SZSE"));
                break;
            default:
                dates.addAll(stockInfoMapper.selectDistinctSnapshotDates(firstDay, lastDay));
                dates.addAll(stockDailyMapper.selectDistinctTradeDates(firstDay, lastDay));
                break;
        }
        return dates;
    }
}
