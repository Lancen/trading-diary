package com.tradingdiary.mapper;

import com.tradingdiary.service.market.SectorMarginDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 板块两融占比 Mapper，提供板块级别两融占比聚合查询
 */
@Mapper
public interface SectorMarginMapper {

    /**
     * 聚合指定板块在日期范围内的两融占比日数据（含 LAG 窗口函数计算变动）
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT）
     * @param sectorCode 板块代码
     * @param startDate  起始日期（含），null 时不限
     * @param endDate    结束日期（含），null 时不限
     * @return 板块两融占比日数据列表
     */
    List<SectorMarginDaily> selectSectorMarginAggregate(@Param("sectorType") String sectorType,
                                                          @Param("sectorCode") String sectorCode,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
}