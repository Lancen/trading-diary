package com.tradingdiary.service;

import com.tradingdiary.collection.model.GapReportVO;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginMacroMapper;
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
    private final MarginMacroMapper marginMacroMapper;

    public GapDetectionService(TradeCalendarMapper tradeCalendarMapper,
                                MarginDailyMapper marginDailyMapper,
                                MarginMacroMapper marginMacroMapper) {
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.marginDailyMapper = marginDailyMapper;
        this.marginMacroMapper = marginMacroMapper;
    }

    /**
     * 检测指定日期范围内的数据缺口
     * <p>
     * 对比交易日历和实际采集的两融数据，返回缺失的交易日数据。
     * 结果按周分组，便于查看每周的数据完整性。
     * </p>
     *
     * @param start 开始日期
     * @param end 结束日期
     * @param dataType 数据类型（MARGIN_DAILY_SSE/SZSE 或 MARGIN_MACRO_SSE/SZSE）
     * @return 数据缺口报告，包含每周的完整度统计和缺失日期列表
     */
    public GapReportVO getGaps(LocalDate start, LocalDate end, String dataType) {
        List<TradeCalendar> tradingDays = tradeCalendarMapper.selectTradingDays(start, end);
        Set<LocalDate> expectedDates = tradingDays.stream()
                .map(TradeCalendar::getTradeDate)
                .collect(Collectors.toCollection(TreeSet::new));

        String exchange = extractExchange(dataType);
        List<LocalDate> collectedDates = isMacro(dataType)
                ? marginMacroMapper.selectDistinctTradeDates(start, end, exchange)
                : marginDailyMapper.selectDistinctTradeDates(start, end, exchange);
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

    /**
     * 按ISO周标准对日期进行分组
     * <p>
     * 将日期集合按"年份-W周数"的格式分组，便于按周统计。
     * </p>
     *
     * @param expectedDates 需要分组的日期集合
     * @return 按周分组的日期映射，key格式为"YYYY-Www"
     */
    private static String extractExchange(String dataType) {
        if (dataType == null) return "SSE";
        return dataType.endsWith("SZSE") ? "SZSE" : "SSE";
    }

    private static boolean isMacro(String dataType) {
        return dataType != null && dataType.startsWith("MARGIN_MACRO");
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