package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ConceptIndustryVO {
    private String code;
    private String name;
    private Integer stockCount;
    private BigDecimal marginBalance;
    private BigDecimal marginChange;
    private BigDecimal shortBalance;
    private BigDecimal shortChange;
    private LocalDate snapDate;
}
