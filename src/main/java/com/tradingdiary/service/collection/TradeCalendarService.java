package com.tradingdiary.service.collection;

/**
 * 交易日历同步服务，封装交易日历数据的同步逻辑
 */
public interface TradeCalendarService {

    /**
     * 同步交易日历数据
     *
     * @return 同步的交易日记录数
     */
    int syncTradeCalendar();
}
