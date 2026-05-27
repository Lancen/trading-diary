package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SectorIndexDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 板块指数日线 Mapper，提供板块指数日线数据的查询
 */
@Mapper
public interface SectorIndexDailyMapper extends BaseMapper<SectorIndexDaily> {

    /**
     * 按板块类型查询最大交易日期
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT）
     * @return 该类型的最大交易日期，无数据时返回 null
     */
    @Select("SELECT MAX(trade_date) FROM sector_index_daily WHERE is_deleted = 0 AND sector_type = #{sectorType}")
    LocalDate selectMaxTradeDateByType(@Param("sectorType") String sectorType);
}
