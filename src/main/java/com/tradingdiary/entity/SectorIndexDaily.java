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
 * 板块指数日线数据，记录行业/概念板块指数的 OHLCV 交易数据
 */
@Getter
@Setter
@TableName("sector_index_daily")
public class SectorIndexDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 板块类型（INDUSTRY/CONCEPT） */
    private String sectorType;

    /** 板块代码 */
    private String sectorCode;

    /** 交易日期 */
    private LocalDate tradeDate;

    /** 开盘价 */
    private BigDecimal open;

    /** 最高价 */
    private BigDecimal high;

    /** 最低价 */
    private BigDecimal low;

    /** 收盘价 */
    private BigDecimal close;

    /** 成交量（股） */
    private Long volume;

    /** 成交额（元） */
    private BigDecimal amount;

    /** 涨跌幅（%） */
    private BigDecimal changePct;

    /** 成交量占全市场比例（%，非数据库字段，查询时计算） */
    @TableField(exist = false)
    private BigDecimal volumePct;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
