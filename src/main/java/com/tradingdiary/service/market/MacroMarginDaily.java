package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MacroMarginDaily(
        LocalDate tradeDate,
        BigDecimal marginBalance,
        BigDecimal shortBalance
) {
}
