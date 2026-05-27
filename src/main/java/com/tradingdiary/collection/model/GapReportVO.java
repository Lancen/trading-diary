package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据缺口报告视图对象，按周汇总缺失交易日
 */
@Getter
@Setter
public class GapReportVO {

    /** 按周分组的缺口列表 */
    private List<WeekGap> weeks;

    /** 总周数 */
    private int totalWeeks;

    /** 数据完整的周数 */
    private int completeWeeks;

    /** 部分缺失的周数 */
    private int partialWeeks;

    /** 完全缺失的周数 */
    private int missingWeeks;

    /**
     * 单周缺口详情，记录该周内缺失的交易日
     */
    @Getter
    @Setter
    public static class WeekGap {

        /** 周起始日期 */
        private LocalDate weekStart;

        /** 周结束日期 */
        private LocalDate weekEnd;

        /** 该周应采集天数 */
        private int expectedDays;

        /** 该周已采集天数 */
        private int collectedDays;

        /** 缺失日期列表 */
        private List<String> missingDates;

        /** 周状态（complete/partial/missing） */
        private String status;
    }
}
