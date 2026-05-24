package com.tradingdiary.service.market.impl;

import com.tradingdiary.service.market.SectorMarginDaily;
import com.tradingdiary.service.market.SectorMarginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SectorMarginServiceImpl implements SectorMarginService {

    private static final Logger log = LoggerFactory.getLogger(SectorMarginServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public SectorMarginServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SectorMarginDaily> aggregate(String sectorType, String sectorCode, LocalDate startDate, LocalDate endDate) {
        String sectorTable = "INDUSTRY".equals(sectorType) ? "stock_industry" : "stock_concept";
        String sectorCodeColumn = "INDUSTRY".equals(sectorType) ? "industry_code" : "concept_code";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.trade_date, ? AS sector_type, ? AS sector_code, ")
           .append("SUM(m.margin_balance) AS margin_balance, ")
           .append("SUM(m.short_balance) AS short_balance, ")
           .append("SUM(m.total_balance) AS total_balance ")
           .append("FROM margin_daily m ")
           .append("INNER JOIN ").append(sectorTable).append(" s ON m.stock_code = s.stock_code AND s.is_deleted = 0 ")
           .append("WHERE s.").append(sectorCodeColumn).append(" = ? AND m.is_deleted = 0 ");

        if (startDate != null) {
            sql.append("AND m.trade_date >= ? ");
        }
        if (endDate != null) {
            sql.append("AND m.trade_date <= ? ");
        }
        sql.append("GROUP BY m.trade_date ORDER BY m.trade_date ASC");

        int paramIndex = 0;
        Object[] params;
        if (startDate != null && endDate != null) {
            params = new Object[]{sectorType, sectorCode, sectorCode, startDate, endDate};
        } else if (startDate != null) {
            params = new Object[]{sectorType, sectorCode, sectorCode, startDate};
        } else if (endDate != null) {
            params = new Object[]{sectorType, sectorCode, sectorCode, endDate};
        } else {
            params = new Object[]{sectorType, sectorCode, sectorCode};
        }

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new SectorMarginDaily(
                rs.getDate("trade_date").toLocalDate(),
                rs.getString("sector_type"),
                rs.getString("sector_code"),
                rs.getBigDecimal("margin_balance"),
                rs.getBigDecimal("short_balance"),
                rs.getBigDecimal("total_balance")
        ), params);
    }
}
