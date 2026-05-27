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

/**
 * 两融明细数据，记录个股级别的融资融券交易明细
 */
@Getter
@Setter
@TableName("margin_daily")
public class MarginDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 股票代码（6位纯数字） */
    private String stockCode;

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 交易所（SSE/SZSE） */
    private String exchange;

    /** 融资余额（元） */
    private BigDecimal marginBalance;

    /** 融资买入额（元） */
    private BigDecimal marginBuy;

    /** 融资偿还额（元） */
    private BigDecimal marginRepay;

    /** 融券余额（元） */
    private BigDecimal shortBalance;

    /** 融券卖出量（股） */
    private Long shortSellVol;

    /** 融券偿还量（股） */
    private Long shortRepayVol;

    /** 融券余量（股） */
    private Long shortRemainVol;

    /** 两融余额合计（元） */
    private BigDecimal totalBalance;

    /** 融资余额变动（元） */
    private BigDecimal marginChange;

    /** 融券余额变动（元） */
    private BigDecimal shortChange;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
