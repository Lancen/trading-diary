package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.ClassificationChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 分类变更日志 Mapper，提供股票行业/概念分类变更记录的查询
 */
@Mapper
public interface ClassificationChangeLogMapper extends BaseMapper<ClassificationChangeLog> {

    /**
     * 根据股票代码和分类类型查询变更日志
     *
     * @param stockCode 股票代码
     * @param type      分类类型（INDUSTRY 或 CONCEPT）
     * @return 变更日志列表，按快照日期倒序排列
     */
    @Select("SELECT * FROM classification_change_log WHERE stock_code = #{stockCode} AND classification_type = #{type} ORDER BY snap_date DESC")
    List<ClassificationChangeLog> selectByStockAndType(@Param("stockCode") String stockCode, @Param("type") String type);

    /**
     * 查询指定日期范围内的分类变更日志
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 变更日志列表，按快照日期倒序排列
     */
    @Select("SELECT * FROM classification_change_log WHERE snap_date BETWEEN #{start} AND #{end} ORDER BY snap_date DESC")
    List<ClassificationChangeLog> selectByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
