package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarginDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 融资融券个股明细 Mapper，提供个股融资融券数据的查询与汇总统计
 */
@Mapper
public interface MarginDailyMapper extends BaseMapper<MarginDaily> {

    /**
     * 查询指定日期范围和交易所的去重交易日期列表
     *
     * @param start    起始日期（含）
     * @param end      结束日期（含）
     * @param exchange 交易所代码（如 SSE、SZSE）
     * @return 去重的交易日期列表，按日期升序排列
     */
    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("exchange") String exchange);

    /**
     * 汇总指定日期的融资余额
     *
     * @param tradeDate 交易日期，为空时汇总全部
     * @return 融资余额合计，无记录时返回 0
     */
    BigDecimal sumMarginBalance(@Param("tradeDate") String tradeDate);

    /**
     * 汇总指定日期的融券余额
     *
     * @param tradeDate 交易日期，为空时汇总全部
     * @return 融券余额合计，无记录时返回 0
     */
    BigDecimal sumShortBalance(@Param("tradeDate") String tradeDate);

    /**
     * 汇总指定日期的融资融券余额合计
     *
     * @param tradeDate 交易日期，为空时汇总全部
     * @return 余额合计，无记录时返回 0
     */
    BigDecimal sumTotalBalance(@Param("tradeDate") String tradeDate);

    /**
     * 统计指定日期的去重股票数量
     *
     * @param tradeDate 交易日期，为空时统计全部
     * @return 去重股票数量
     */
    Integer countDistinctStocks(@Param("tradeDate") String tradeDate);

    /**
     * 查询指定交易所融资融券明细表的最大交易日期
     *
     * @param exchange 交易所代码（如 SSE、SZSE）
     * @return 最大交易日期，无记录时返回 null
     */
    LocalDate selectMaxTradeDate(@Param("exchange") String exchange);
}
