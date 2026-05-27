package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票列表视图对象，用于股票列表展示及两融数据概览
 */
@Getter
@Setter
public class StockListVO {
    /** 股票代码 */
    private String stockCode;
    /** 股票名称 */
    private String stockName;
    /** 所属行业 */
    private String industry;
    /** 关联概念（逗号分隔） */
    private String concepts;
    /** 收盘价 */
    private BigDecimal close;
    /** 涨跌幅（%） */
    private BigDecimal changePct;
    /** 成交量 */
    private Long volume;
    /** 融资余额 */
    private BigDecimal marginBalance;
    /** 融资余额变动 */
    private BigDecimal marginChange;
    /** 融券余额 */
    private BigDecimal shortBalance;
    /** 融券余额变动 */
    private BigDecimal shortChange;
    /** 交易日期 */
    private LocalDate tradeDate;
}
