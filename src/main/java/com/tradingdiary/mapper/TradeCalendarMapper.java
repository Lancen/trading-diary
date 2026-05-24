package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.TradeCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 交易日历 Mapper，提供交易日查询和日历日期范围检索
 */
@Mapper
public interface TradeCalendarMapper extends BaseMapper<TradeCalendar> {

    /**
     * 查询指定日期范围内的所有交易日
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 交易日列表，按日期升序排列
     */
    List<TradeCalendar> selectTradingDays(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 查询交易日历表中的最大日期
     *
     * @return 最大日期，无记录时返回 null
     */
    LocalDate selectMaxCalDate();
}
