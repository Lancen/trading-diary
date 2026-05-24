package com.tradingdiary.service.collection;

import com.tradingdiary.collection.model.MarginSummaryVO;

import java.time.LocalDate;

/**
 * 融资统计服务，封装两融数据汇总查询
 */
public interface MarginStatsService {

    /**
     * 获取指定交易日的融资统计总量
     *
     * @param tradeDate 交易日期，为 null 时查询全部数据
     * @return 融资统计汇总信息
     */
    MarginSummaryVO getMarginSummary(LocalDate tradeDate);
}
