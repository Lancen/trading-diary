package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.TradeCalendarService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 交易日历采集处理器
 */
@Component
public class TradeCalendarHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final TradeCalendarService tradeCalendarService;

    public TradeCalendarHandler(AKToolsClient aktoolsClient, TradeCalendarService tradeCalendarService) {
        this.aktoolsClient = aktoolsClient;
        this.tradeCalendarService = tradeCalendarService;
    }

    @Override
    public String dataType() {
        return "TRADE_CALENDAR";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        return aktoolsClient.fetchTradeCalendar();
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return tradeCalendarService.syncTradeCalendar();
    }
}