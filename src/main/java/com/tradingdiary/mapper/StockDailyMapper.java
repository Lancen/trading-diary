package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.StockDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StockDailyMapper extends BaseMapper<StockDaily> {

    @Select("SELECT DISTINCT trade_date FROM stock_daily WHERE trade_date BETWEEN #{start} AND #{end} AND is_deleted = 0 ORDER BY trade_date")
    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
