package com.tradingdiary.service.market.impl;

import com.tradingdiary.mapper.SectorCorrelationMapper;
import com.tradingdiary.service.market.SectorCorrelation;
import com.tradingdiary.service.market.SectorCorrelationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 板块关联度服务实现，基于成分股交集计算 Jaccard 相似度
 */
@Service
public class SectorCorrelationServiceImpl implements SectorCorrelationService {

    private static final Set<String> VALID_SECTOR_TYPES = Set.of("INDUSTRY", "CONCEPT");

    private static final Map<String, String> TARGET_TYPE_MAP = Map.of(
            "INDUSTRY", "CONCEPT",
            "CONCEPT", "INDUSTRY"
    );

    private static final int MAX_RESULTS = 20;

    private static final BigDecimal MIN_JACCARD = new BigDecimal("0.05");

    private final SectorCorrelationMapper sectorCorrelationMapper;

    public SectorCorrelationServiceImpl(SectorCorrelationMapper sectorCorrelationMapper) {
        this.sectorCorrelationMapper = sectorCorrelationMapper;
    }

    @Override
    public List<SectorCorrelation> compute(String sectorType, String sectorCode) {
        if (!VALID_SECTOR_TYPES.contains(sectorType)) {
            throw new IllegalArgumentException("Invalid sectorType: " + sectorType);
        }

        String targetType = TARGET_TYPE_MAP.get(sectorType);

        List<Map<String, Object>> rankings = sectorCorrelationMapper.selectIntersectionRanking(
                sectorType, sectorCode, MAX_RESULTS);

        return rankings.stream()
                .map(row -> {
                    String relatedCode = (String) row.get("relatedCode");
                    String relatedName = (String) row.get("relatedName");
                    long intersectionCount = ((Number) row.get("intersectionCount")).longValue();
                    long sourceCount = ((Number) row.get("sourceCount")).longValue();
                    Long targetCount = sectorCorrelationMapper.selectStockCount(sectorType, relatedCode);
                    if (targetCount == null) targetCount = 0L;

                    long unionSize = sourceCount + targetCount - intersectionCount;
                    BigDecimal jaccard = unionSize > 0
                            ? BigDecimal.valueOf(intersectionCount).divide(BigDecimal.valueOf(unionSize), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return new SectorCorrelation(targetType, relatedCode, relatedName, jaccard, intersectionCount, sourceCount, targetCount);
                })
                .filter(c -> c.jaccard().compareTo(MIN_JACCARD) >= 0)
                .toList();
    }
}