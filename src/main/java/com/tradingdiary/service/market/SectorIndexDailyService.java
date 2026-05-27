package com.tradingdiary.service.market;

import com.tradingdiary.entity.SectorIndexDaily;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 板块指数日线查询服务，提供板块指数日线数据查询和成交占比计算
 */
public interface SectorIndexDailyService {

    /** 合法的板块类型 */
    Set<String> VALID_SECTOR_TYPES = Set.of("INDUSTRY", "CONCEPT");

    /**
     * 查询板块指数日线数据，并计算成交占比
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT）
     * @param sectorCode 板块代码
     * @param startDate 起始日期（含），null 时不限
     * @param endDate 结束日期（含），null 时不限
     * @return 板块指数日线数据列表（含 volumePct）
     */
    List<SectorIndexDaily> query(String sectorType, String sectorCode,
                                  LocalDate startDate, LocalDate endDate);
}