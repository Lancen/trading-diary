package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.TushareClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.StockDailyCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 股票日线 Tushare 历史补采处理器
 */
@Component
public class StockDailyTushareHandler implements DataTypeHandler {

    private final TushareClient tushareClient;
    private final StockDailyCleanseService stockDailyCleanseService;

    public StockDailyTushareHandler(TushareClient tushareClient, StockDailyCleanseService stockDailyCleanseService) {
        this.tushareClient = tushareClient;
        this.stockDailyCleanseService = stockDailyCleanseService;
    }

    @Override
    public String dataType() {
        return "STOCK_DAILY_TUSHARE";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        return FetchResult.single(tushareClient.fetchDaily(tradeDate));
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return stockDailyCleanseService.cleanseTushareDaily(rawJson);
    }
}