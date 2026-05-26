package com.tradingdiary.service.market.impl;

import com.tradingdiary.service.market.SectorMarginDaily;
import com.tradingdiary.service.market.SectorMarginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SectorMarginServiceImpl implements SectorMarginService {

    private static final Logger log = LoggerFactory.getLogger(SectorMarginServiceImpl.class);

    private static final Map<String, String> SECTOR_TABLE_MAP = Map.of(
            "INDUSTRY", "stock_industry",
            "CONCEPT", "stock_concept"
    );

    private static final Map<String, String> SECTOR_CODE_COLUMN_MAP = Map.of(
            "INDUSTRY", "industry_code",
            "CONCEPT", "concept_code"
    );

    private static final Set<String> VALID_SECTOR_TYPES = SECTOR_TABLE_MAP.keySet();

    private final JdbcTemplate jdbcTemplate;

    public SectorMarginServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SectorMarginDaily> aggregate(String sectorType, String sectorCode, LocalDate startDate, LocalDate endDate) {
        if (!VALID_SECTOR_TYPES.contains(sectorType)) {
            throw new IllegalArgumentException("Invalid sectorType: " + sectorType);
        }

        String sectorTable = SECTOR_TABLE_MAP.get(sectorType);
        String sectorCodeColumn = SECTOR_CODE_COLUMN_MAP.get(sectorType);

        String innerSql = "SELECT m.trade_date, "
                + "SUM(m.margin_balance) AS margin_balance, "
                + "SUM(m.short_balance) AS short_balance, "
                + "SUM(m.total_balance) AS total_balance "
                + "FROM margin_daily m "
                + "INNER JOIN " + sectorTable + " s ON m.stock_code = s.stock_code AND s.is_deleted = 0 "
                + "WHERE s." + sectorCodeColumn + " = ? AND m.is_deleted = 0 ";

        List<Object> innerParams = new ArrayList<>();
        innerParams.add(sectorCode);
        if (startDate != null) {
            innerSql += "AND m.trade_date >= ? ";
            innerParams.add(startDate);
        }
        if (endDate != null) {
            innerSql += "AND m.trade_date <= ? ";
            innerParams.add(endDate);
        }
        innerSql += "GROUP BY m.trade_date ";

        String sql = "SELECT trade_date, ? AS sector_type, ? AS sector_code, "
                + "margin_balance, short_balance, total_balance, "
                + "margin_balance - LAG(margin_balance) OVER (ORDER BY trade_date) AS margin_balance_change, "
                + "short_balance - LAG(short_balance) OVER (ORDER BY trade_date) AS short_balance_change, "
                + "total_balance - LAG(total_balance) OVER (ORDER BY trade_date) AS total_balance_change "
                + "FROM (" + innerSql + ") sub "
                + "ORDER BY trade_date ASC";

        List<Object> params = new ArrayList<>();
        params.add(sectorType);
        params.add(sectorCode);
        params.addAll(innerParams);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SectorMarginDaily(
                rs.getDate("trade_date").toLocalDate(),
                rs.getString("sector_type"),
                rs.getString("sector_code"),
                rs.getBigDecimal("margin_balance"),
                rs.getBigDecimal("short_balance"),
                rs.getBigDecimal("total_balance"),
                rs.getBigDecimal("margin_balance_change"),
                rs.getBigDecimal("short_balance_change"),
                rs.getBigDecimal("total_balance_change")
        ), params.toArray());
    }
}
