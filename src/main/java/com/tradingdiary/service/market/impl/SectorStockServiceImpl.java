package com.tradingdiary.service.market.impl;

import com.tradingdiary.service.market.SectorStockItem;
import com.tradingdiary.service.market.SectorStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 板块成分股查询服务实现，提供行业/概念板块的成分股列表
 */
@Service
public class SectorStockServiceImpl implements SectorStockService {

    private static final Logger log = LoggerFactory.getLogger(SectorStockServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public SectorStockServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SectorStockItem> listIndustryStocks(String industryCode) {
        String sql = "SELECT si.stock_code, MAX(info.name) AS stock_name "
                   + "FROM stock_industry si "
                   + "LEFT JOIN stock_info info ON si.stock_code = info.stock_code AND info.is_deleted = 0 "
                   + "WHERE si.industry_code = ? AND si.is_deleted = 0 "
                   + "GROUP BY si.stock_code "
                   + "ORDER BY si.stock_code";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SectorStockItem(
                rs.getString("stock_code"),
                rs.getString("stock_name")
        ), industryCode);
    }

    @Override
    public List<SectorStockItem> listConceptStocks(String conceptCode) {
        String sql = "SELECT sc.stock_code, MAX(info.name) AS stock_name "
                   + "FROM stock_concept sc "
                   + "LEFT JOIN stock_info info ON sc.stock_code = info.stock_code AND info.is_deleted = 0 "
                   + "WHERE sc.concept_code = ? AND sc.is_deleted = 0 "
                   + "GROUP BY sc.stock_code "
                   + "ORDER BY sc.stock_code";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SectorStockItem(
                rs.getString("stock_code"),
                rs.getString("stock_name")
        ), conceptCode);
    }
}
