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
 * 数据采集日志，记录每次 FETCH/CLEANSE 任务的执行状态和结果
 */
@Getter
@Setter
@TableName("data_collection_log")
public class DataCollectionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据类型标识 */
    private String dataType;

    /** 任务类型（FETCH/CLEANSE） */
    private String jobType;

    /** 执行状态（RUNNING/SUCCESS/FAILED） */
    private String status;

    /** 交易日 */
    private LocalDate tradeDate;

    /** 周起始日期 */
    private LocalDate weekStart;

    /** 周结束日期 */
    private LocalDate weekEnd;

    /** 处理的记录数 */
    private Integer recordCount;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParams;

    /** 错误消息 */
    private String errorMsg;

    /** 备注 */
    private String remark;

    /** 任务开始时间 */
    private LocalDateTime startedAt;

    /** 任务完成时间 */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
