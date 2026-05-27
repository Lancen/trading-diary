package com.tradingdiary.service.market.impl;

import com.tradingdiary.service.market.MacroMarginDaily;
import com.tradingdiary.service.market.MacroMarginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 两融总量查询服务实现，聚合市场级融资融券数据
 */
@Service
public class MacroMarginServiceImpl implements MacroMarginService {

    private static final Logger log = LoggerFactory.getLogger(MacroMarginServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public MacroMarginServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MacroMarginDaily> aggregate(LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT trade_date, ")
           .append("SUM(margin_balance) AS margin_balance, ")
           .append("SUM(short_balance) AS short_balance ")
           .append("FROM margin_macro WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (startDate != null) {
            sql.append("AND trade_date >= ? ");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append("AND trade_date <= ? ");
            params.add(endDate);
        }
        sql.append("GROUP BY trade_date ORDER BY trade_date ASC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new MacroMarginDaily(
                rs.getDate("trade_date").toLocalDate(),
                rs.getBigDecimal("margin_balance"),
                rs.getBigDecimal("short_balance")
        ), params.toArray());
    }
}
