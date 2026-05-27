package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 交易日历日视图，表示某一天是否为交易日及数据采集状态
 */
@Getter
@Setter
public class CalendarDayVO {
    /** 日期 */
    private LocalDate date;
    /** 是否为交易日 */
    private boolean isTradingDay;
    /** 该日期是否有采集数据 */
    private boolean hasData;
    /** 采集状态描述 */
    private String status;
}
