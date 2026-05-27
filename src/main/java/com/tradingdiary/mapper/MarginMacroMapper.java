package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarginMacro;
import com.tradingdiary.service.market.MacroMarginDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 融资融券宏观数据 Mapper，提供市场整体融资融券汇总数据的查询
 */
@Mapper
public interface MarginMacroMapper extends BaseMapper<MarginMacro> {

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
     * 查询指定交易所融资融券宏观数据的最大交易日期
     *
     * @param exchange 交易所代码（如 SSE、SZSE）
     * @return 最大交易日期，无记录时返回 null
     */
    LocalDate selectMaxTradeDate(@Param("exchange") String exchange);

    /**
     * 聚合全市场两融总量日数据（按日期分组求和）
     *
     * @param startDate 起始日期（含），null 时不限
     * @param endDate   结束日期（含），null 时不限
     * @return 两融总量日数据列表
     */
    List<MacroMarginDaily> selectMacroMarginDailyAggregate(@Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);
}
