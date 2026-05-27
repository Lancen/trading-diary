package com.tradingdiary.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.SectorRanking;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 板块排名控制器，提供行业/概念板块排名查询
 */
@RestController
@RequestMapping("/api/v1/admin/sector-ranking")
@PreAuthorize("hasRole('ADMIN')")
public class SectorRankingController {

    private static final Map<String, String> NAME_TABLE_MAP = Map.of(
            "INDUSTRY", "industry",
            "CONCEPT", "concept"
    );

    private static final Set<String> VALID_TYPES = NAME_TABLE_MAP.keySet();

    private final JdbcTemplate jdbcTemplate;

    public SectorRankingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "查询板块排名")
    @GetMapping
    public ApiResponse<List<SectorRanking>> query(
            @RequestParam String sectorType,
            @RequestParam(required = false) LocalDate tradeDate,
            @RequestParam(defaultValue = "amount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        if (!VALID_TYPES.contains(sectorType)) {
            return ApiResponse.fail(400, "Invalid sectorType: " + sectorType);
        }

        if (tradeDate == null) {
            tradeDate = jdbcTemplate.queryForObject(
                    "SELECT MAX(trade_date) FROM sector_index_daily WHERE sector_type = ? AND is_deleted = 0",
                    LocalDate.class, sectorType);
            if (tradeDate == null) return ApiResponse.ok(List.of());
        }

        LocalDate prevDate = jdbcTemplate.queryForObject(
                "SELECT MAX(trade_date) FROM sector_index_daily WHERE sector_type = ? AND trade_date < ? AND is_deleted = 0",
                LocalDate.class, sectorType, tradeDate);

        String nameTable = NAME_TABLE_MAP.get(sectorType);
        String nameCol = "INDUSTRY".equals(sectorType) ? "industry_code" : "concept_code";

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("SELECT cur.sector_code, n.name AS sector_name, cur.trade_date, ")
                .append("cur.amount, cur.change_pct, ")
                .append("CASE WHEN prev.amount IS NOT NULL THEN cur.amount - prev.amount ELSE NULL END AS amount_change, ")
                .append("CASE WHEN prev.change_pct IS NOT NULL THEN cur.change_pct - prev.change_pct ELSE NULL END AS change_pct_change ")
                .append("FROM sector_index_daily cur ")
                .append("INNER JOIN ").append(nameTable).append(" n ON cur.sector_code = n.code ")
                .append("LEFT JOIN sector_index_daily prev ON cur.sector_code = prev.sector_code ")
                .append("AND prev.sector_type = ? AND prev.trade_date = ? AND prev.is_deleted = 0 ")
                .append("WHERE cur.sector_type = ? AND cur.trade_date = ? AND cur.is_deleted = 0 ");

        params.add(sectorType);
        params.add(prevDate);
        params.add(sectorType);
        params.add(tradeDate);

        String sortColumn = switch (sortBy) {
            case "amountChange" -> "amount_change";
            case "changePct" -> "cur.change_pct";
            case "changePctChange" -> "change_pct_change";
            default -> "cur.amount";
        };

        String direction = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        sql.append("ORDER BY ").append(sortColumn).append(" ").append(direction);

        BigDecimal mktAmount = null;
        try {
            mktAmount = jdbcTemplate.queryForObject(
                    "SELECT SUM(amount) FROM market_index_daily WHERE trade_date = ? AND is_deleted = 0",
                    BigDecimal.class, tradeDate);
        } catch (Exception ignored) {}

        final LocalDate finalTradeDate = tradeDate;
        final BigDecimal finalMktAmount = mktAmount;

        List<SectorRanking> result = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            BigDecimal amount = rs.getBigDecimal("amount");
            BigDecimal amountChange = rs.getBigDecimal("amount_change");
            BigDecimal changePct = rs.getBigDecimal("change_pct");
            BigDecimal changePctChange = rs.getBigDecimal("change_pct_change");
            BigDecimal volumePct = null;
            if (finalMktAmount != null && finalMktAmount.compareTo(BigDecimal.ZERO) > 0 && amount != null) {
                volumePct = amount.divide(finalMktAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            }
            return new SectorRanking(
                    sectorType,
                    rs.getString("sector_code"),
                    rs.getString("sector_name"),
                    finalTradeDate,
                    amount, amountChange, changePct, changePctChange, volumePct
            );
        }, params.toArray());

        return ApiResponse.ok(result);
    }
}
