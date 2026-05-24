package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarginMacro;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MarginMacroMapper extends BaseMapper<MarginMacro> {

    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("exchange") String exchange);

    LocalDate selectMaxTradeDate(@Param("exchange") String exchange);
}
