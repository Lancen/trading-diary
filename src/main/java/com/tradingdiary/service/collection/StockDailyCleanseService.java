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
