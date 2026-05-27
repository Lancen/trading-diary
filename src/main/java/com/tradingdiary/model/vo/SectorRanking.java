package com.tradingdiary.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 板块排名 VO，包含板块成交额、涨跌幅及其变动、成交占比
 */
public record SectorRanking(
        String sectorType,
        String sectorCode,
        String sectorName,
        LocalDate tradeDate,
        BigDecimal amount,
        BigDecimal amountChange,
        BigDecimal changePct,
        BigDecimal changePctChange,
        BigDecimal volumePct
) {}