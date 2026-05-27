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
 * 板块分类变更日志，记录成分股的进出变更
 */
@Getter
@Setter
@TableName("classification_change_log")
public class ClassificationChangeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 股票代码 */
    private String stockCode;

    /** 分类类型：INDUSTRY-行业，CONCEPT-概念 */
    private String classificationType;

    /** 板块代码 */
    private String sectorCode;

    /** 变更动作：ADD-加入，REMOVE-移出 */
    private String action;

    /** 快照日期 */
    private LocalDate snapDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
