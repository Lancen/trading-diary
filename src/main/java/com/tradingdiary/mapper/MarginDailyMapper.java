package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarginDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MarginDailyMapper extends BaseMapper<MarginDaily> {

    @Select("SELECT DISTINCT trade_date FROM margin_daily WHERE trade_date BETWEEN #{start} AND #{end} AND exchange = #{exchange} AND is_deleted = 0 ORDER BY trade_date")
    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("exchange") String exchange);

    @Select("<script>SELECT COALESCE(SUM(margin_balance), 0) FROM margin_daily WHERE is_deleted = 0<if test='tradeDate != null'> AND trade_date = #{tradeDate}</if></script>")
    java.math.BigDecimal sumMarginBalance(@Param("tradeDate") String tradeDate);

    @Select("<script>SELECT COALESCE(SUM(short_balance), 0) FROM margin_daily WHERE is_deleted = 0<if test='tradeDate != null'> AND trade_date = #{tradeDate}</if></script>")
    java.math.BigDecimal sumShortBalance(@Param("tradeDate") String tradeDate);

    @Select("<script>SELECT COALESCE(SUM(total_balance), 0) FROM margin_daily WHERE is_deleted = 0<if test='tradeDate != null'> AND trade_date = #{tradeDate}</if></script>")
    java.math.BigDecimal sumTotalBalance(@Param("tradeDate") String tradeDate);

    @Select("<script>SELECT COUNT(DISTINCT stock_code) FROM margin_daily WHERE is_deleted = 0<if test='tradeDate != null'> AND trade_date = #{tradeDate}</if></script>")
    Integer countDistinctStocks(@Param("tradeDate") String tradeDate);
}
