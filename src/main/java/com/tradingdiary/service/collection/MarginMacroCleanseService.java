package com.tradingdiary.service.collection;

/**
 * 两融宏观数据清洗服务，封装原始两融宏观数据的解析与入库逻辑
 */
public interface MarginMacroCleanseService {

    /**
     * 清洗两融宏观数据原始JSON
     *
     * @param rawJson 原始JSON数据
     * @param exchange 交易所代码（SSE/SZSE）
     * @return 清洗并保存的记录数
     */
    int cleanse(String rawJson, String exchange);
}
