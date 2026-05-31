package com.tradingdiary.collection.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockFinancialOverview(
        String stockCode,
        String stockName,
        LocalDate latestReportDate,
        BigDecimal operatingRevenue,
        BigDecimal npParent,
        BigDecimal deductedNp,
        BigDecimal grossMargin,
        BigDecimal netMargin,
        BigDecimal roe,
        BigDecimal debtRatio,
        BigDecimal operatingCashFlow,
        BigDecimal freeCashFlow,
        BigDecimal revenueYoy,
        BigDecimal npYoy,
        BigDecimal peTtm,
        BigDecimal pb,
        BigDecimal totalMv
) {
}
