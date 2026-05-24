package com.tradingdiary.service.collection;

import java.time.LocalDate;

/**
 * 概念板块清洗服务，封装概念板块名称与成分股数据的解析入库逻辑
 */
public interface ConceptCleanseService {

    /**
     * 清洗概念板块名称数据
     *
     * @param rawJson 原始JSON数据
     * @return 清洗并保存的记录数
     */
    int cleanseNames(String rawJson);

    /**
     * 清洗概念板块成分股数据
     *
     * @param rawJson 原始JSON数据
     * @param conceptCode 概念板块代码
     * @param snapDate 快照日期
     * @return 清洗并保存的记录数
     */
    int cleanseCons(String rawJson, String conceptCode, LocalDate snapDate);
}
