package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarginDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MarginDailyMapper extends BaseMapper<MarginDaily> {

    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("exchange") String exchange);

    BigDecimal sumMarginBalance(@Param("tradeDate") String tradeDate);

    BigDecimal sumShortBalance(@Param("tradeDate") String tradeDate);

    BigDecimal sumTotalBalance(@Param("tradeDate") String tradeDate);

    Integer countDistinctStocks(@Param("tradeDate") String tradeDate);

    LocalDate selectMaxTradeDate(@Param("exchange") String exchange);
}
