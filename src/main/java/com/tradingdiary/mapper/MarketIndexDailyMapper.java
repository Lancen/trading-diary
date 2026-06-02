package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarketIndexDaily;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

@Mapper
public interface MarketIndexDailyMapper extends BaseMapper<MarketIndexDaily> {

    LocalDate selectMaxTradeDate();
}
