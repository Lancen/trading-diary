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

@Getter
@Setter
@TableName("data_collection_log")
public class DataCollectionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dataType;

    private String jobType;

    private String status;

    private LocalDate tradeDate;

    private LocalDate weekStart;

    private LocalDate weekEnd;

    private Integer recordCount;

    private String requestUrl;

    private String requestParams;

    private String errorMsg;

    private String remark;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
