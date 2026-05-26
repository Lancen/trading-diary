package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

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
