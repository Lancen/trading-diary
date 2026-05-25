package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarketIndexDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface MarketIndexDailyMapper extends BaseMapper<MarketIndexDaily> {

    @Select("SELECT MAX(trade_date) FROM market_index_daily WHERE is_deleted = 0")
    LocalDate selectMaxTradeDate();
}
