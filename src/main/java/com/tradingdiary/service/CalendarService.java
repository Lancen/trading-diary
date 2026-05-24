package com.tradingdiary.service;

import java.util.Map;

/**
 * 交易日历服务，封装月度交易日历与数据采集状态查询逻辑
 */
public interface CalendarService {

    /**
     * 获取指定月份的交易日历
     *
     * @param year 年份
     * @param month 月份
     * @param dataType 数据类型，用于标记对应数据的采集状态
     * @return 月度日历数据，包含每日是否为交易日及数据采集状态
     */
    Map<String, Object> getMonthCalendar(int year, int month, String dataType);
}
