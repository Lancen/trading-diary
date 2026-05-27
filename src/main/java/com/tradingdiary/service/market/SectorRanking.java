package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 板块排名 VO
 */
public record SectorRanking(
        String sectorType,        // 板块类型（industry/concept）
        String sectorCode,        // 板块代码
        String sectorName,        // 板块名称
        LocalDate tradeDate,      // 交易日期
        BigDecimal amount,        // 成交额
        BigDecimal amountChange,  // 成交额变动
        BigDecimal changePct,     // 涨跌幅
        BigDecimal changePctChange, // 涨跌幅变动
        BigDecimal volumePct      // 成交占比
) {}
