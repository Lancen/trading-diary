package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class StockListVO {
    private String stockCode;
    private String stockName;
    private String industry;
    private String concepts;
    private BigDecimal close;
    private BigDecimal changePct;
    private Long volume;
    private BigDecimal marginBalance;
    private BigDecimal marginChange;
    private BigDecimal shortBalance;
    private BigDecimal shortChange;
    private LocalDate tradeDate;
}
