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
@TableName("raw_data")
public class RawData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long collectionLogId;

    private String dataType;

    private LocalDate tradeDate;

    private String source;

    private String sectorCode;

    private String rawJson;

    private LocalDateTime fetchAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
