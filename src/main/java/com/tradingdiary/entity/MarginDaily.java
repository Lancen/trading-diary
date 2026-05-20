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
@TableName("margin_daily")
public class MarginDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;

    private LocalDate tradeDate;

    private String exchange;

    private BigDecimal marginBalance;

    private BigDecimal marginBuy;

    private BigDecimal marginRepay;

    private BigDecimal shortBalance;

    private Long shortSellVol;

    private Long shortRepayVol;

    private Long shortRemainVol;

    private BigDecimal totalBalance;

    private BigDecimal marginChange;

    private BigDecimal shortChange;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
