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
 * 股票基本信息与实时行情快照
 */
@Getter
@Setter
@TableName("stock_info")
public class StockInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 股票代码（含市场前缀，如 SH600000） */
    private String code;

    /** 市场标识（SH/SZ） */
    private String market;

    /** 股票代码（6位纯数字） */
    private String stockCode;

    /** 股票名称 */
    private String name;

    /** 最新价（元） */
    private BigDecimal latestPrice;

    /** 涨跌幅（%） */
    private BigDecimal changePct;

    /** 涨跌额（元） */
    private BigDecimal changeAmount;

    /** 成交量（股） */
    private Long volume;

    /** 成交额（元） */
    private BigDecimal amount;

    /** 换手率（%） */
    private BigDecimal turnoverRate;

    /** 量比 */
    private BigDecimal volumeRatio;

    /** 市盈率 */
    private BigDecimal pe;

    /** 市净率 */
    private BigDecimal pb;

    /** 总市值（元） */
    private BigDecimal totalMv;

    /** 流通市值（元） */
    private BigDecimal floatMv;

    /** 行情快照日期 */
    private LocalDate snapshotDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
