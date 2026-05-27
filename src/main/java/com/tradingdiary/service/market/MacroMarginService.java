package com.tradingdiary.service.market;

import java.time.LocalDate;
import java.util.List;

/**
 * 两融总量查询服务，聚合市场级融资融券数据
 */
public interface MacroMarginService {

    /**
     * 聚合指定日期范围的两融总量日数据
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 两融总量日数据列表
     */
    List<MacroMarginDaily> aggregate(LocalDate startDate, LocalDate endDate);
}
