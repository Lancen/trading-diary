package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class StockDetailVO {
    private String stockCode;
    private String stockName;
    private String industry;
    private List<String> concepts;
    private LatestQuote latestQuote;
    private LatestMargin latestMargin;
    private List<DailyKline> dailyKlines;
    private List<DailyMargin> dailyMargins;

    @Getter
    @Setter
    public static class LatestQuote {
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private BigDecimal changePct;
    }

    @Getter
    @Setter
    public static class LatestMargin {
        private BigDecimal marginBalance;
        private BigDecimal marginBuy;
        private BigDecimal shortBalance;
        private BigDecimal totalBalance;
    }

    @Getter
    @Setter
    public static class DailyKline {
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
    }

    @Getter
    @Setter
    public static class DailyMargin {
        private LocalDate tradeDate;
        private BigDecimal marginBalance;
        private BigDecimal marginChange;
        private BigDecimal shortBalance;
        private BigDecimal shortChange;
    }
}
