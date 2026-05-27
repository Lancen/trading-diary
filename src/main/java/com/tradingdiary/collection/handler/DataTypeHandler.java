package com.tradingdiary.collection.handler;

import java.time.LocalDate;

/**
 * 采集类型处理器策略接口，封装特定 dataType 的 FETCH 和 CLEANSE 逻辑
 */
public interface DataTypeHandler {

    /** 该 handler 负责的 dataType 标识（如 "STOCK_SPOT"、"MARGIN_DAILY_SSE"） */
    String dataType();

    /**
     * FETCH 阶段：调用外部 API 获取原始数据
     *
     * @param tradeDate 交易日期
     * @return 原始 JSON 字符串
     */
    String fetch(LocalDate tradeDate);

    /**
     * CLEANSE 阶段：解析原始数据并写入业务表
     *
     * @param rawJson 原始 JSON 字符串
     * @param tradeDate 交易日期
     * @return 写入的记录数
     */
    int cleanse(String rawJson, LocalDate tradeDate);
}