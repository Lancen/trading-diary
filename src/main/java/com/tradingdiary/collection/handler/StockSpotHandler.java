package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import com.tradingdiary.service.collection.StockInfoCleanseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 股票行情采集处理器（含日线），一次 spot API 调用同时写入 stock_info 和 stock_daily
 */
@Component
public class StockSpotHandler implements DataTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(StockSpotHandler.class);

    private final AKToolsClient aktoolsClient;
    private final StockInfoCleanseService stockInfoCleanseService;
    private final StockDailyCleanseService stockDailyCleanseService;

    public StockSpotHandler(AKToolsClient aktoolsClient,
                            StockInfoCleanseService stockInfoCleanseService,
                            StockDailyCleanseService stockDailyCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.stockInfoCleanseService = stockInfoCleanseService;
        this.stockDailyCleanseService = stockDailyCleanseService;
    }

    @Override
    public String dataType() {
        return "STOCK_SPOT";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        return aktoolsClient.fetchStockSpot();
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        int infoCount = stockInfoCleanseService.cleanse(rawJson, tradeDate);
        int dailyCount = stockDailyCleanseService.cleanse(rawJson, tradeDate);
        log.info("StockSpot cleanse: {} stock_info + {} stock_daily records", infoCount, dailyCount);
        return infoCount + dailyCount;
    }
}