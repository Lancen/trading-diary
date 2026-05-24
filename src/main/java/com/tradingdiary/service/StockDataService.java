package com.tradingdiary.service;

import com.tradingdiary.collection.model.StockDetailVO;
import com.tradingdiary.collection.model.StockListVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 股票数据查询服务，封装股票列表分页查询与详情获取逻辑
 */
public interface StockDataService {

    /**
     * 分页查询股票列表
     *
     * @param keyword 关键词筛选
     * @param industry 行业筛选
     * @param concept 概念筛选
     * @param tradeDate 交易日期筛选
     * @param sortBy 排序字段
     * @param sortDir 排序方向（asc/desc）
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果，包含股票列表和分页信息
     */
    Map<String, Object> listStocks(String keyword, String industry, String concept,
                                   LocalDate tradeDate, String sortBy, String sortDir, int page, int size);

    /**
     * 获取股票详情
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 股票详情信息
     */
    StockDetailVO getStockDetail(String stockCode, LocalDate startDate, LocalDate endDate);
}
