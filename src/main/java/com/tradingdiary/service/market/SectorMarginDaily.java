package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 板块两融占比日数据 VO
 */
public record SectorMarginDaily(
        LocalDate tradeDate,              // 交易日期
        String sectorType,                // 板块类型（industry/concept）
        String sectorCode,                // 板块代码
        BigDecimal marginBalance,         // 融资余额
        BigDecimal shortBalance,          // 融券余额
        BigDecimal totalBalance,          // 两融余额合计
        BigDecimal marginBalanceChange,   // 融资余额变动
        BigDecimal shortBalanceChange,    // 融券余额变动
        BigDecimal totalBalanceChange     // 两融余额合计变动
) {
}
