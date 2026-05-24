package com.tradingdiary.service;

import java.util.Map;

public interface CalendarService {

    Map<String, Object> getMonthCalendar(int year, int month, String dataType);
}
