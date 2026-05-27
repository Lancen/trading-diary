package com.tradingdiary.collection.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 板块（行业/概念）视图对象，用于板块列表展示及两融数据聚合
 */
@Getter
@Setter
public class ConceptIndustryVO {
    /** 板块代码 */
    private String code;
    /** 板块名称 */
    private String name;
    /** 成分股数量 */
    private Integer stockCount;
    /** 融资余额 */
    private BigDecimal marginBalance;
    /** 融资余额变动 */
    private BigDecimal marginChange;
    /** 融券余额 */
    private BigDecimal shortBalance;
    /** 融券余额变动 */
    private BigDecimal shortChange;
    /** 成交额占比（%） */
    private BigDecimal volumePct;
    /** 快照日期 */
    private LocalDate snapDate;
    /** 是否置顶 */
    private Boolean pinned;
    /** 置顶排序序号 */
    private Integer pinOrder;
}
