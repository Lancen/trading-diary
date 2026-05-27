package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 两融总量日数据 VO
 */
public record MacroMarginDaily(
        LocalDate tradeDate,       // 交易日期
        BigDecimal marginBalance,  // 融资余额
        BigDecimal shortBalance    // 融券余额
) {
}
