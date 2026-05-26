package com.tradingdiary.service.market;

import java.math.BigDecimal;

public record SectorCorrelation(
        String relatedType,
        String relatedCode,
        String relatedName,
        BigDecimal jaccard,
        long intersectionCount,
        long sourceCount,
        long targetCount
) {}
