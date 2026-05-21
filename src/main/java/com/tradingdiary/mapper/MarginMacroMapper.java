package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.MarginMacro;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MarginMacroMapper extends BaseMapper<MarginMacro> {

    @Select("SELECT DISTINCT trade_date FROM margin_macro WHERE trade_date BETWEEN #{start} AND #{end} AND exchange = #{exchange} AND is_deleted = 0 ORDER BY trade_date")
    List<LocalDate> selectDistinctTradeDates(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("exchange") String exchange);
}
