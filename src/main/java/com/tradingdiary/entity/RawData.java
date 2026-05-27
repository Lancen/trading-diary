package com.tradingdiary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 原始采集数据，存储外部 API 返回的 JSON 原文
 */
@Getter
@Setter
@TableName("raw_data")
public class RawData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联采集日志ID */
    private Long collectionLogId;

    /** 数据类型标识 */
    private String dataType;

    /** 交易日 */
    private LocalDate tradeDate;

    /** 数据来源（如 AKTools） */
    private String source;

    /** 板块代码（板块指数日线专用） */
    private String sectorCode;

    /** 原始 JSON 数据 */
    private String rawJson;

    /** 采集时间 */
    private LocalDateTime fetchAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
