package com.tradingdiary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 两融汇总数据，记录市场级别的融资融券合计数据
 */
@Getter
@Setter
@TableName("margin_macro")
public class MarginMacro implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 交易所（SSE/SZSE） */
    private String exchange;

    /** 融资买入额（元） */
    private BigDecimal marginBuy;

    /** 融资余额（元） */
    private BigDecimal marginBalance;

    /** 融券卖出量（股） */
    private Long shortSellVol;

    /** 融券余量（股） */
    private Long shortRemainVol;

    /** 融券余额（元） */
    private BigDecimal shortBalance;

    /** 两融余额合计（元） */
    private BigDecimal totalBalance;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
