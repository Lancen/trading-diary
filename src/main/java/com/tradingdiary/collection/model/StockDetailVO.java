package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 股票详情视图对象，包含最新行情、两融数据及历史K线/两融日线
 */
@Getter
@Setter
public class StockDetailVO {
    /** 股票代码 */
    private String stockCode;
    /** 股票名称 */
    private String stockName;
    /** 所属行业 */
    private String industry;
    /** 关联概念列表 */
    private List<String> concepts;
    /** 最新行情 */
    private LatestQuote latestQuote;
    /** 最新两融数据 */
    private LatestMargin latestMargin;
    /** 日K线数据 */
    private List<DailyKline> dailyKlines;
    /** 日两融数据 */
    private List<DailyMargin> dailyMargins;

    /**
     * 最新行情快照
     */
    @Getter
    @Setter
    public static class LatestQuote {
        /** 开盘价 */
        private BigDecimal open;
        /** 最高价 */
        private BigDecimal high;
        /** 最低价 */
        private BigDecimal low;
        /** 收盘价 */
        private BigDecimal close;
        /** 成交量 */
        private Long volume;
        /** 涨跌幅（%） */
        private BigDecimal changePct;
    }

    /**
     * 最新两融数据快照
     */
    @Getter
    @Setter
    public static class LatestMargin {
        /** 融资余额 */
        private BigDecimal marginBalance;
        /** 融资买入额 */
        private BigDecimal marginBuy;
        /** 融券余额 */
        private BigDecimal shortBalance;
        /** 两融余额合计 */
        private BigDecimal totalBalance;
    }

    /**
     * 日K线数据点
     */
    @Getter
    @Setter
    public static class DailyKline {
        /** 交易日期 */
        private LocalDate tradeDate;
        /** 开盘价 */
        private BigDecimal open;
        /** 最高价 */
        private BigDecimal high;
        /** 最低价 */
        private BigDecimal low;
        /** 收盘价 */
        private BigDecimal close;
        /** 成交量 */
        private Long volume;
    }

    /**
     * 日两融数据点
     */
    @Getter
    @Setter
    public static class DailyMargin {
        /** 交易日期 */
        private LocalDate tradeDate;
        /** 融资余额 */
        private BigDecimal marginBalance;
        /** 融资余额变动 */
        private BigDecimal marginChange;
        /** 融券余额 */
        private BigDecimal shortBalance;
        /** 融券余额变动 */
        private BigDecimal shortChange;
    }
}
