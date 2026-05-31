package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.FinancialStatementCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class StockCashFlowHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final FinancialStatementCleanseService cleanseService;

    public StockCashFlowHandler(AKToolsClient aktoolsClient,
                                 FinancialStatementCleanseService cleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.cleanseService = cleanseService;
    }

    @Override
    public String dataType() {
        return "STOCK_CASH_FLOW";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        String startDate = tradeDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return FetchResult.single(aktoolsClient.fetchStockCashFlow("603986", startDate, endDate));
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return cleanseService.cleanseCashFlow(rawJson);
    }

    @Override
    public boolean requiresCalendar() {
        return false;
    }
}
