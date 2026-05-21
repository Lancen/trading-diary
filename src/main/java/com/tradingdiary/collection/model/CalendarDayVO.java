package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CalendarDayVO {
    private LocalDate date;
    private boolean isTradingDay;
    private boolean hasData;
    private String status;
}
