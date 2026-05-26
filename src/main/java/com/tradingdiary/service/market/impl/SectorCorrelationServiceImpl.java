package com.tradingdiary.service.market.impl;

import com.tradingdiary.service.market.SectorCorrelation;
import com.tradingdiary.service.market.SectorCorrelationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SectorCorrelationServiceImpl implements SectorCorrelationService {

    private static final Map<String, String> SOURCE_TABLE_MAP = Map.of(
            "INDUSTRY", "stock_industry",
            "CONCEPT", "stock_concept"
    );

    private static final Map<String, String> SOURCE_CODE_COLUMN_MAP = Map.of(
            "INDUSTRY", "industry_code",
            "CONCEPT", "concept_code"
    );

    private static final Map<String, String> TARGET_TABLE_MAP = Map.of(
            "INDUSTRY", "stock_industry",
            "CONCEPT", "stock_concept"
    );

    private static final Map<String, String> TARGET_CODE_COLUMN_MAP = Map.of(
            "INDUSTRY", "industry_code",
            "CONCEPT", "concept_code"
    );

    private static final Map<String, String> TARGET_NAME_TABLE_MAP = Map.of(
            "INDUSTRY", "industry",
            "CONCEPT", "concept"
    );

    static final Map<String, String> TARGET_TYPE_MAP = Map.of(
            "INDUSTRY", "CONCEPT",
            "CONCEPT", "INDUSTRY"
    );

    private static final Set<String> VALID_SECTOR_TYPES = SOURCE_TABLE_MAP.keySet();

    private static final int MAX_RESULTS = 20;

    private static final BigDecimal MIN_JACCARD = new BigDecimal("0.05");

    private final JdbcTemplate jdbcTemplate;

    public SectorCorrelationServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SectorCorrelation> compute(String sectorType, String sectorCode) {
        if (!VALID_SECTOR_TYPES.contains(sectorType)) {
            throw new IllegalArgumentException("Invalid sectorType: " + sectorType);
        }

        String targetType = TARGET_TYPE_MAP.get(sectorType);
        String sourceTable = SOURCE_TABLE_MAP.get(sectorType);
        String sourceCodeCol = SOURCE_CODE_COLUMN_MAP.get(sectorType);
        String targetTable = TARGET_TABLE_MAP.get(targetType);
        String targetCodeCol = TARGET_CODE_COLUMN_MAP.get(targetType);
        String targetNameTable = TARGET_NAME_TABLE_MAP.get(targetType);

        String sql = buildCorrelationSql(sourceTable, sourceCodeCol, targetTable, targetCodeCol, targetNameTable);

        String targetCountSql = "SELECT COUNT(DISTINCT stock_code) FROM " + targetTable
                + " WHERE " + targetCodeCol + " = ? AND is_deleted = 0";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String relatedCode = rs.getString("related_code");
            String relatedName = rs.getString("related_name");
            long intersectionCount = rs.getLong("intersection_count");
            long sourceCount = rs.getLong("source_count");
            Long targetCount = jdbcTemplate.queryForObject(targetCountSql, Long.class, relatedCode);
            if (targetCount == null) targetCount = 0L;

            long unionSize = sourceCount + targetCount - intersectionCount;
            BigDecimal jaccard = unionSize > 0
                    ? BigDecimal.valueOf(intersectionCount).divide(BigDecimal.valueOf(unionSize), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new SectorCorrelation(targetType, relatedCode, relatedName, jaccard, intersectionCount, sourceCount, targetCount);
        }, sectorCode, sectorCode, MAX_RESULTS).stream()
                .filter(c -> c.jaccard().compareTo(MIN_JACCARD) >= 0)
                .toList();
    }

    private String buildCorrelationSql(String sourceTable, String sourceCodeCol,
                                        String targetTable, String targetCodeCol,
                                        String targetNameTable) {
        return "SELECT t." + targetCodeCol + " AS related_code, "
                + "n.name AS related_name, "
                + "COUNT(DISTINCT s.stock_code) AS intersection_count, "
                + "src_cnt.source_total AS source_count "
                + "FROM " + sourceTable + " s "
                + "INNER JOIN " + targetTable + " t ON s.stock_code = t.stock_code AND t.is_deleted = 0 "
                + "INNER JOIN " + targetNameTable + " n ON t." + targetCodeCol + " = n.code "
                + "INNER JOIN ("
                + "  SELECT COUNT(DISTINCT stock_code) AS source_total "
                + "  FROM " + sourceTable
                + "  WHERE " + sourceCodeCol + " = ? AND is_deleted = 0"
                + ") src_cnt "
                + "WHERE s." + sourceCodeCol + " = ? AND s.is_deleted = 0 "
                + "GROUP BY t." + targetCodeCol + ", n.name, src_cnt.source_total "
                + "HAVING COUNT(DISTINCT s.stock_code) > 0 "
                + "ORDER BY intersection_count DESC "
                + "LIMIT ?";
    }
}
