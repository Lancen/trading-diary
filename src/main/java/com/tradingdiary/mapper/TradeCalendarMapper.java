package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.TradeCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TradeCalendarMapper extends BaseMapper<TradeCalendar> {

    List<TradeCalendar> selectTradingDays(@Param("start") LocalDate start, @Param("end") LocalDate end);

    LocalDate selectMaxCalDate();
}
