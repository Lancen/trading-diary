package com.tradingdiary.service.collection;

import java.time.LocalDate;

/**
 * 股票基本信息清洗服务，封装原始股票信息的解析与入库逻辑
 */
public interface StockInfoCleanseService {

    /**
     * 清洗股票基本信息原始JSON
     *
     * @param rawJson 原始JSON数据
     * @param snapshotDate 快照日期
     * @return 清洗并保存的记录数
     */
    int cleanse(String rawJson, LocalDate snapshotDate);
}
