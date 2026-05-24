package com.tradingdiary.service.collection;

import java.time.LocalDate;

/**
 * 两融数据清洗服务，封装原始两融数据的解析与入库逻辑
 */
public interface MarginCleanseService {

    /**
     * 清洗两融数据原始JSON
     *
     * @param rawJson 原始JSON数据
     * @param exchange 交易所代码（SSE/SZSE）
     * @param tradeDate 交易日期
     * @return 清洗并保存的记录数
     */
    int cleanse(String rawJson, String exchange, LocalDate tradeDate);
}
