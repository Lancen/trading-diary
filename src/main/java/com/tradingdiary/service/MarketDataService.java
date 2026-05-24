package com.tradingdiary.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 市场数据查询服务，封装概念板块与行业板块的分页查询逻辑
 */
public interface MarketDataService {

    /**
     * 分页查询概念板块列表
     *
     * @param keyword 关键词筛选
     * @param tradeDate 交易日期筛选
     * @param sortBy 排序字段
     * @param sortDir 排序方向（asc/desc）
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果，包含概念板块列表和分页信息
     */
    Map<String, Object> listConcepts(String keyword, LocalDate tradeDate,
                                     String sortBy, String sortDir, int page, int size);

    /**
     * 分页查询行业板块列表
     *
     * @param keyword 关键词筛选
     * @param tradeDate 交易日期筛选
     * @param sortBy 排序字段
     * @param sortDir 排序方向（asc/desc）
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果，包含行业板块列表和分页信息
     */
    Map<String, Object> listIndustries(String keyword, LocalDate tradeDate,
                                       String sortBy, String sortDir, int page, int size);
}
