package com.tradingdiary.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 板块关联度 Mapper，提供板块间成分股交集查询
 */
@Mapper
public interface SectorCorrelationMapper {

    /**
     * 查询与指定板块有共同成分股的关联板块列表（含交集数和源板块总数）
     *
     * @param sectorType       板块类型（INDUSTRY/CONCEPT）
     * @param targetSectorCode 目标板块代码
     * @param maxResults       返回条数上限
     * @return map 列表，每条含 relatedCode、relatedName、intersectionCount、sourceCount
     */
    List<Map<String, Object>> selectIntersectionRanking(@Param("sectorType") String sectorType,
                                                          @Param("targetSectorCode") String targetSectorCode,
                                                          @Param("maxResults") int maxResults);

    /**
     * 查询指定板块的成分股总数
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT）
     * @param sectorCode 板块代码
     * @return 成分股数量
     */
    Long selectStockCount(@Param("sectorType") String sectorType,
                          @Param("sectorCode") String sectorCode);
}