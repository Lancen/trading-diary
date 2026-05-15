package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.ClassificationChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ClassificationChangeLogMapper extends BaseMapper<ClassificationChangeLog> {

    @Select("SELECT * FROM classification_change_log WHERE stock_code = #{stockCode} AND classification_type = #{type} ORDER BY snap_date DESC")
    List<ClassificationChangeLog> selectByStockAndType(@Param("stockCode") String stockCode, @Param("type") String type);

    @Select("SELECT * FROM classification_change_log WHERE snap_date BETWEEN #{start} AND #{end} ORDER BY snap_date DESC")
    List<ClassificationChangeLog> selectByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
