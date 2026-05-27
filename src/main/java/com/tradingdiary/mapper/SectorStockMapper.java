package com.tradingdiary.mapper;

import com.tradingdiary.service.market.SectorStockItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 板块成分股 Mapper，提供板块与股票关联的查询
 */
@Mapper
public interface SectorStockMapper {

    /**
     * 查询行业板块的成分股列表
     *
     * @param industryCode 行业板块代码
     * @return 成分股条目列表
     */
    List<SectorStockItem> selectIndustryStocks(@Param("industryCode") String industryCode);

    /**
     * 查询概念板块的成分股列表
     *
     * @param conceptCode 概念板块代码
     * @return 成分股条目列表
     */
    List<SectorStockItem> selectConceptStocks(@Param("conceptCode") String conceptCode);
}