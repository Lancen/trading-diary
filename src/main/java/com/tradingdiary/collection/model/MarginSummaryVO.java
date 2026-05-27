package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 两融汇总视图对象，展示市场级融资融券余额统计
 */
@Getter
@Setter
public class MarginSummaryVO {
    /** 融资余额合计 */
    private BigDecimal totalMarginBalance;
    /** 融券余额合计 */
    private BigDecimal totalShortBalance;
    /** 两融余额合计 */
    private BigDecimal totalBalance;
    /** 股票数量 */
    private Integer stockCount;
    /** 交易日期 */
    private LocalDate tradeDate;
}
