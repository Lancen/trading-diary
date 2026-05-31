package com.tradingdiary.service.collection;

import com.tradingdiary.entity.StockIncome;
import com.tradingdiary.entity.StockBalanceSheet;
import com.tradingdiary.entity.StockCashFlow;
import com.tradingdiary.entity.StockFinancialIndicator;

import java.util.List;

public interface FinancialStatementCleanseService {

    int cleanseIncome(String rawJson);

    int cleanseBalanceSheet(String rawJson);

    int cleanseCashFlow(String rawJson);

    int cleanseFinancialIndicator(String rawJson);

    List<StockIncome> parseIncome(String rawJson);

    List<StockBalanceSheet> parseBalanceSheet(String rawJson);

    List<StockCashFlow> parseCashFlow(String rawJson);

    List<StockFinancialIndicator> parseFinancialIndicator(String rawJson);
}
