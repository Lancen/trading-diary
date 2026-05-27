package com.tradingdiary.service.market;

import java.util.List;

/**
 * 板块成分股查询服务，提供行业/概念板块的成分股列表
 */
public interface SectorStockService {

    /**
     * 查询行业板块的成分股列表
     *
     * @param industryCode 行业板块代码
     * @return 成分股条目列表
     */
    List<SectorStockItem> listIndustryStocks(String industryCode);

    /**
     * 查询概念板块的成分股列表
     *
     * @param conceptCode 概念板块代码
     * @return 成分股条目列表
     */
    List<SectorStockItem> listConceptStocks(String conceptCode);
}
