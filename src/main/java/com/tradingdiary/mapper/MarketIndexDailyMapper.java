package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarketIndexDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 宽基指数日线 Mapper，提供指数日线数据的查询
 */
@Mapper
public interface MarketIndexDailyMapper extends BaseMapper<MarketIndexDaily> {

    /**
     * 查询宽基指数日线表的最大交易日期
     *
     * @return 最大交易日期，无数据时返回 null
     */
    @Select("SELECT MAX(trade_date) FROM market_index_daily WHERE is_deleted = 0")
    LocalDate selectMaxTradeDate();
}
