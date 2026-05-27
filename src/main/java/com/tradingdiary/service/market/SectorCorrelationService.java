package com.tradingdiary.service.market;

import java.util.List;

/**
 * 板块关联度服务，计算板块间的相关性指标
 */
public interface SectorCorrelationService {

    /**
     * 计算指定板块与其他板块的关联度
     *
     * @param sectorType 板块类型（industry/concept）
     * @param sectorCode 板块代码
     * @return 关联度列表，按 Jaccard 系数降序排列
     */
    List<SectorCorrelation> compute(String sectorType, String sectorCode);
}
