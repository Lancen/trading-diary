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
@TableName("stock_info")
public class StockInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String market;

    private String stockCode;

    private String name;

    private BigDecimal latestPrice;

    private BigDecimal changePct;

    private BigDecimal changeAmount;

    private Long volume;

    private BigDecimal amount;

    private BigDecimal turnoverRate;

    private BigDecimal volumeRatio;

    private BigDecimal pe;

    private BigDecimal pb;

    private BigDecimal totalMv;

    private BigDecimal floatMv;

    private LocalDate snapshotDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
