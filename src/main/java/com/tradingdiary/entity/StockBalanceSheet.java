package com.tradingdiary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("stock_balance_sheet")
public class StockBalanceSheet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private BigDecimal totalAssets;
    private BigDecimal currentAssets;
    private BigDecimal cash;
    private BigDecimal accountsReceivable;
    private BigDecimal inventory;
    private BigDecimal fixedAssets;
    private BigDecimal goodwill;
    private BigDecimal totalLiabilities;
    private BigDecimal currentLiabilities;
    private BigDecimal interestBearingDebt;
    private BigDecimal totalEquity;
    private BigDecimal equityParent;
    private BigDecimal minorityInterest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
