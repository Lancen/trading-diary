package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.TradeCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TradeCalendarMapper extends BaseMapper<TradeCalendar> {

    @Select("SELECT * FROM trade_calendar WHERE trade_date BETWEEN #{start} AND #{end} AND is_trading_day = 1 AND is_deleted = 0 ORDER BY trade_date")
    List<TradeCalendar> selectTradingDays(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT MAX(trade_date) FROM trade_calendar WHERE is_deleted = 0")
    LocalDate selectMaxCalDate();
}
