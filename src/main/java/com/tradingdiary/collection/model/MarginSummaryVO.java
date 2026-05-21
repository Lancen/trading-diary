package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MarginSummaryVO {
    private BigDecimal totalMarginBalance;
    private BigDecimal totalShortBalance;
    private BigDecimal totalBalance;
    private Integer stockCount;
    private LocalDate tradeDate;
}
