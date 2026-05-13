package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.StockDaily;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockDailyMapper extends BaseMapper<StockDaily> {
}
