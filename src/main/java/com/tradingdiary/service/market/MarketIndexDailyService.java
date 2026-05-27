package com.tradingdiary.service.market;

import com.tradingdiary.entity.MarketIndexDaily;

import java.time.LocalDate;
import java.util.List;

/**
 * 宽基指数日线查询服务，提供指数行情查询
 */
public interface MarketIndexDailyService {

    /**
     * 查询指定指数在日期范围内的日线数据
     *
     * @param indexCode 指数代码
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 指数日线数据列表
     */
    List<MarketIndexDaily> query(String indexCode, LocalDate startDate, LocalDate endDate);

    /**
     * 查询所有指数的最新日线数据
     *
     * @return 各指数最新日线数据列表
     */
    List<MarketIndexDaily> latest();
}
