package com.tradingdiary.service;

import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class GapDetectionService {

    private static final Logger log = LoggerFactory.getLogger(GapDetectionService.class);

    private final TradeCalendarMapper tradeCalendarMapper;
    private final MarginDailyMapper marginDailyMapper;

    public GapDetectionService(TradeCalendarMapper tradeCalendarMapper,
                                MarginDailyMapper marginDailyMapper) {
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.marginDailyMapper = marginDailyMapper;
    }

    /**
     * 检测指定日期范围内的数据缺口
     */
    public GapReportVO getGaps(LocalDate start, LocalDate end, String exchange) {
        List<TradeCalendar> tradingDays = tradeCalendarMapper.selectTradingDays(start, end);
        Set<LocalDate> expectedDates = tradingDays.stream()
                .map(TradeCalendar::getTradeDate)
                .collect(Collectors.toCollection(TreeSet::new));

        List<LocalDate> collectedDates = marginDailyMapper.selectDistinctTradeDates(start, end, exchange);
        Set<LocalDate> collectedSet = new TreeSet<>(collectedDates);

        Set<LocalDate> missingDates = new TreeSet<>(expectedDates);
        missingDates.removeAll(collectedSet);

        Map<String, List<LocalDate>> weekGroups = groupByIsoWeek(expectedDates);

        List<GapReportVO.WeekGap> weeks = new ArrayList<>();
        int completeWeeks = 0;
        int partialWeeks = 0;
        int missingWeeks = 0;

        for (Map.Entry<String, List<LocalDate>> entry : weekGroups.entrySet()) {
            List<LocalDate> weekDates = entry.getValue();
            int expectedDays = weekDates.size();
            long collectedDays = weekDates.stream().filter(collectedSet::contains).count();
            List<String> missing = weekDates.stream()
                    .filter(d -> !collectedSet.contains(d))
                    .map(LocalDate::toString)
                    .sorted()
                    .collect(Collectors.toList());

            String status;
            if (missing.isEmpty()) {
                status = "COMPLETE";
                completeWeeks++;
            } else if (collectedDays == 0) {
                status = "MISSING";
                missingWeeks++;
            } else {
                status = "PARTIAL";
                partialWeeks++;
            }

            GapReportVO.WeekGap weekGap = new GapReportVO.WeekGap();
            weekGap.setWeekStart(weekDates.get(0));
            weekGap.setWeekEnd(weekDates.get(weekDates.size() - 1));
            weekGap.setExpectedDays(expectedDays);
            weekGap.setCollectedDays((int) collectedDays);
            weekGap.setMissingDates(missing);
            weekGap.setStatus(status);
            weeks.add(weekGap);
        }

        GapReportVO report = new GapReportVO();
        report.setWeeks(weeks);
        report.setTotalWeeks(weeks.size());
        report.setCompleteWeeks(completeWeeks);
        report.setPartialWeeks(partialWeeks);
        report.setMissingWeeks(missingWeeks);

        return report;
    }

    private Map<String, List<LocalDate>> groupByIsoWeek(Set<LocalDate> expectedDates) {
        WeekFields weekFields = WeekFields.ISO;

        Map<String, List<LocalDate>> grouped = new LinkedHashMap<>();
        for (LocalDate date : expectedDates) {
            int weekYear = date.get(weekFields.weekBasedYear());
            int weekNum = date.get(weekFields.weekOfWeekBasedYear());
            String key = weekYear + "-W" + String.format("%02d", weekNum);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(date);
        }

        return grouped;
    }
}