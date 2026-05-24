package com.tradingdiary.service.collection;

import com.tradingdiary.entity.StockDaily;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日行情清洗服务，封装原始行情数据的解析与入库逻辑
 */
public interface StockDailyCleanseService {

    /**
     * 清洗单日股票行情原始数据
     *
     * @param rawJson 原始JSON数据
     * @param tradeDate 交易日期
     * @return 清洗并保存的记录数
     */
    int cleanse(String rawJson, LocalDate tradeDate);

    /**
     * 批量清洗历史股票行情数据
     *
     * @param rawJsonList 原始JSON数据列表
     * @param stockCodes 对应的股票代码列表
     * @return 清洗并保存的总记录数
     */
    int cleanseHistBatch(List<String> rawJsonList, List<String> stockCodes);

    /**
     * 清洗单只股票的历史行情JSON数据
     *
     * @param rawJson 原始JSON数据
     * @param stockCode 股票代码
     * @return 清洗并保存的记录数
     */
    int cleanseHistJson(String rawJson, String stockCode);

    /**
     * 清洗Tushare日行情接口返回的数据
     *
     * @param tushareResponse Tushare接口返回的JSON字符串
     * @return 清洗并保存的记录数
     */
    int cleanseTushareDaily(String tushareResponse);

    /**
     * 解析Tushare日行情接口返回的数据为实体列表
     *
     * @param tushareResponse Tushare接口返回的JSON字符串
     * @return 解析后的股票日行情实体列表
     */
    List<StockDaily> parseTushareDaily(String tushareResponse);
}
