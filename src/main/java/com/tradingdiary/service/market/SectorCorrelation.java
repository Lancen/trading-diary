package com.tradingdiary.service.market;

import java.math.BigDecimal;

/**
 * 板块关联度 VO
 */
public record SectorCorrelation(
        String relatedType,        // 关联板块类型（industry/concept）
        String relatedCode,        // 关联板块代码
        String relatedName,        // 关联板块名称
        BigDecimal jaccard,        // Jaccard 相似系数
        long intersectionCount,    // 交集成分股数量
        long sourceCount,          // 源板块成分股数量
        long targetCount           // 目标板块成分股数量
) {}
