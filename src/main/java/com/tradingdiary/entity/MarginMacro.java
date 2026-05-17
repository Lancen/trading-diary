package com.tradingdiary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("margin_macro")
public class MarginMacro implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate tradeDate;

    /** SSE / SZSE */
    private String exchange;

    private BigDecimal marginBuy;

    private BigDecimal marginBalance;

    private Long shortSellVol;

    private Long shortRemainVol;

    private BigDecimal shortBalance;

    private BigDecimal totalBalance;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
