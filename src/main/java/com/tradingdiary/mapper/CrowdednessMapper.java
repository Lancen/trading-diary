package com.tradingdiary.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 拥挤度 Mapper，提供拥挤度计算所需的数据查询
 */
@Mapper
public interface CrowdednessMapper {

    /**
     * 查询指定日期范围内每个交易日的拥挤度数据
     * <p>
     * 返回每日的：trade_date, total_amount, top_amount, total_stocks, top_stocks
     * top_amount = 成交额排名前5%个股的总成交额
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每日拥挤度聚合数据
     */
    List<Map<String, Object>> selectCrowdednessDaily(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * 查询最新交易日期
     *
     * @return 最新交易日期
     */
    LocalDate selectLatestTradeDate();
}
