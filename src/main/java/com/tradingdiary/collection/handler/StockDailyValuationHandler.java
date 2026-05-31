package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.DailyValuationCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StockDailyValuationHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final DailyValuationCleanseService cleanseService;

    public StockDailyValuationHandler(AKToolsClient aktoolsClient,
                                       DailyValuationCleanseService cleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.cleanseService = cleanseService;
    }

    @Override
    public String dataType() {
        return "STOCK_DAILY_VALUATION";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        String dateStr = tradeDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return FetchResult.single(aktoolsClient.fetchStockDailyValuation(dateStr));
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return cleanseService.cleanse(rawJson);
    }
}
