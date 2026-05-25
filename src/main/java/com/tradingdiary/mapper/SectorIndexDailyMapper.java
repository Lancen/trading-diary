package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SectorIndexDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface SectorIndexDailyMapper extends BaseMapper<SectorIndexDaily> {

    @Select("SELECT MAX(trade_date) FROM sector_index_daily WHERE is_deleted = 0 AND sector_type = #{sectorType}")
    LocalDate selectMaxTradeDateByType(@Param("sectorType") String sectorType);
}
