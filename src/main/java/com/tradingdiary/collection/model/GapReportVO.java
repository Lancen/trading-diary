package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class GapReportVO {

    private List<WeekGap> weeks;

    private int totalWeeks;

    private int completeWeeks;

    private int partialWeeks;

    private int missingWeeks;

    @Getter
    @Setter
    public static class WeekGap {

        private LocalDate weekStart;

        private LocalDate weekEnd;

        private int expectedDays;

        private int collectedDays;

        private List<String> missingDates;

        private String status;
    }
}
