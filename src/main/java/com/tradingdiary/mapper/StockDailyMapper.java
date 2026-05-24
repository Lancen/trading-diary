package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.StockDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票日线行情 Mapper，提供股票日线数据的查询
 */
@Mapper
public interface StockDailyMapper extends BaseMapper<StockDaily> {

    /**
     * 查询指定日期范围内的去重交易日期列表
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 去重的交易日期列表，按日期升序排列
     */
    @Select("SELECT DISTINCT trade_date FROM stock_daily WHERE trade_date BETWEEN #{start} AND #{end} AND is_deleted = 0 ORDER BY trade_date")
    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
