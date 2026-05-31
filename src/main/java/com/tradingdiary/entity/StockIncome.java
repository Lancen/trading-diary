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
@TableName("stock_income")
public class StockIncome implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private LocalDate reportDate;
    private String reportType;
    private BigDecimal totalRevenue;
    private BigDecimal operatingRevenue;
    private BigDecimal totalCost;
    private BigDecimal operatingCost;
    private BigDecimal rdExpense;
    private BigDecimal sellingExpense;
    private BigDecimal adminExpense;
    private BigDecimal financeExpense;
    private BigDecimal operatingProfit;
    private BigDecimal totalProfit;
    private BigDecimal netProfit;
    private BigDecimal npParent;
    private BigDecimal npMinority;
    private BigDecimal deductedNp;
    private BigDecimal basicEps;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
