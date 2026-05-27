package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.model.FetchResult;

import java.time.LocalDate;

/**
 * 数据类型采集处理器接口，每种采集类型实现一个 handler
 */
public interface DataTypeHandler {

    /**
     * 数据类型标识（如 STOCK_INFO、MARGIN_DAILY_SSE）
     */
    String dataType();

    /**
     * FETCH 阶段：从数据源获取原始数据
     *
     * @param tradeDate 交易日期
     * @return FETCH 结果（SINGLE 或 MULTI_SECTOR）
     */
    FetchResult fetch(LocalDate tradeDate);

    /**
     * CLEANSE 阶段：将原始 JSON 转换为业务实体并写入数据库
     *
     * @param rawJson   原始 JSON 字符串
     * @param tradeDate 交易日期
     * @return 写入行数
     */
    int cleanse(String rawJson, LocalDate tradeDate);

    /**
     * 是否需要交易日历（日级采集需要，月级快照不需要）
     */
    default boolean requiresCalendar() { return true; }
}