package com.tradingdiary.service.market;

import java.time.LocalDate;
import java.util.List;

/**
 * 板块两融占比服务，计算板块级别的融资融券占比
 */
public interface SectorMarginService {

    /**
     * 聚合指定板块在日期范围内的两融占比日数据
     *
     * @param sectorType 板块类型（industry/concept）
     * @param sectorCode 板块代码
     * @param startDate  起始日期（含）
     * @param endDate    结束日期（含）
     * @return 板块两融占比日数据列表
     */
    List<SectorMarginDaily> aggregate(String sectorType, String sectorCode, LocalDate startDate, LocalDate endDate);
}
