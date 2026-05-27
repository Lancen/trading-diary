package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 历史数据补采请求参数
 */
@Getter
@Setter
public class BackfillRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据类型（如 MARGIN_DAILY_SSE、INDUSTRY_INDEX_DAILY 等） */
    private String dataType;

    /** 交易所（如 SSE、SZSE），部分数据类型需要指定 */
    private String exchange;

    /** 补采起始日期 */
    private LocalDate startDate;

    /** 补采结束日期 */
    private LocalDate endDate;
}
