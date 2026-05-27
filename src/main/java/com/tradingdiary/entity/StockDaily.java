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
 * 股票日行情数据，记录每只股票每日的 OHLCV 交易数据
 */
@Getter
@Setter
@TableName("stock_daily")
public class StockDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 股票代码（6位纯数字） */
    private String stockCode;

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 开盘价（元） */
    private BigDecimal open;

    /** 最高价（元） */
    private BigDecimal high;

    /** 最低价（元） */
    private BigDecimal low;

    /** 收盘价（元） */
    private BigDecimal close;

    /** 成交量（股） */
    private Long volume;

    /** 成交额（元） */
    private BigDecimal amount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
