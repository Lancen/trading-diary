package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.FinancialStatementCleanseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StockIncomeHandler implements DataTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(StockIncomeHandler.class);

    private final AKToolsClient aktoolsClient;
    private final FinancialStatementCleanseService cleanseService;

    public StockIncomeHandler(AKToolsClient aktoolsClient,
                               FinancialStatementCleanseService cleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.cleanseService = cleanseService;
    }

    @Override
    public String dataType() {
        return "STOCK_INCOME";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        String startDate = tradeDate.minusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = tradeDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return FetchResult.single(aktoolsClient.fetchStockIncome("603986", startDate, endDate));
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return cleanseService.cleanseIncome(rawJson);
    }

    @Override
    public boolean requiresCalendar() {
        return false;
    }
}
