package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SectorMarginDaily(
        LocalDate tradeDate,
        String sectorType,
        String sectorCode,
        BigDecimal marginBalance,
        BigDecimal shortBalance,
        BigDecimal totalBalance
) {
}
