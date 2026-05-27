package com.tradingdiary.collection.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 采集状态视图对象，展示某数据类型的采集进度和最近执行情况
 */
@Getter
@Setter
public class CollectionStatusVO {

    /** 数据类型标识（如 STOCK_INFO、MARGIN_DAILY_SSE 等） */
    private String dataType;

    /** 数据类型中文标签 */
    private String dataTypeLabel;

    /** 最近一次 FETCH 任务状态 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JobStatus lastFetch;

    /** 最近一次 CLEANSE 任务状态 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JobStatus lastCleanse;

    /** 最近数据日期 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private java.time.LocalDateTime lastDataDate;

    /**
     * 任务执行状态，记录单次 FETCH 或 CLEANSE 的执行详情
     */
    @Getter
    @Setter
    public static class JobStatus {

        /** 任务状态（如 RUNNING、SUCCESS、FAILED） */
        private String status;

        /** 任务开始时间 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime startedAt;

        /** 任务完成时间 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime completedAt;

        /** 处理记录数 */
        private Integer recordCount;

        /** 错误信息 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String errorMsg;
    }
}
