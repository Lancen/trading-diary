package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndustryCleanseService {

    private static final Logger log = LoggerFactory.getLogger(IndustryCleanseService.class);

    private final IndustryMapper industryMapper;
    private final StockIndustryMapper stockIndustryMapper;
    private final ObjectMapper objectMapper;

    public IndustryCleanseService(IndustryMapper industryMapper,
                                  StockIndustryMapper stockIndustryMapper,
                                  ObjectMapper objectMapper) {
        this.industryMapper = industryMapper;
        this.stockIndustryMapper = stockIndustryMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Cleanse industry name list from stock_board_industry_name_em response.
     * Saves new industry names to the industry table (simple INSERT — change detection in Phase 6).
     *
     * @param rawJson raw JSON from stock_board_industry_name_em API
     * @return number of new industries inserted
     */
    public int cleanseNames(String rawJson) {
        List<Industry> industries = parseIndustryNames(rawJson);
        if (industries.isEmpty()) {
            log.warn("No industry names parsed from response");
            return 0;
        }

        int count = 0;
        for (Industry industry : industries) {
            Long exists = industryMapper.selectCount(
                    new LambdaQueryWrapper<Industry>()
                            .eq(Industry::getCode, industry.getCode())
            );
            if (exists == 0) {
                industryMapper.insert(industry);
                count++;
            }
        }

        log.info("Industry names cleanse complete: {} new industries inserted", count);
        return count;
    }

    /**
     * Cleanse constituent list for ONE industry from stock_board_industry_cons_em response.
     * For now, simple saveOrUpdate. Change detection will be added in Phase 6.
     *
     * @param rawJson      raw JSON from stock_board_industry_cons_em API
     * @param industryCode the industry board code
     * @param snapDate     the snapshot date
     * @return number of stock-industry relations saved
     */
    public int cleanseCons(String rawJson, String industryCode, LocalDate snapDate) {
        List<StockIndustry> relations = parseStockIndustryList(rawJson, industryCode, snapDate);
        if (relations.isEmpty()) {
            log.info("No constituents parsed for industry {}", industryCode);
            return 0;
        }

        int count = 0;
        for (StockIndustry relation : relations) {
            StockIndustry existing = stockIndustryMapper.selectOne(
                    new LambdaQueryWrapper<StockIndustry>()
                            .eq(StockIndustry::getStockCode, relation.getStockCode())
                            .eq(StockIndustry::getIndustryCode, relation.getIndustryCode())
            );
            if (existing != null) {
                relation.setId(existing.getId());
                stockIndustryMapper.updateById(relation);
            } else {
                stockIndustryMapper.insert(relation);
            }
            count++;
        }

        log.info("Industry cons cleanse complete: {} constituents for {}", count, industryCode);
        return count;
    }

    private List<Industry> parseIndustryNames(String rawJson) {
        List<Industry> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for industry names, got: {}", root.getNodeType());
                return result;
            }
            for (JsonNode node : root) {
                String code = safeText(node, "板块代码");
                String name = safeText(node, "板块名称");
                if (code != null && !code.isEmpty() && name != null && !name.isEmpty()) {
                    Industry industry = new Industry();
                    industry.setCode(code);
                    industry.setName(name);
                    result.add(industry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse industry names JSON", e);
            throw new RuntimeException("Failed to parse industry names: " + e.getMessage(), e);
        }
        return result;
    }

    private List<StockIndustry> parseStockIndustryList(String rawJson, String industryCode, LocalDate snapDate) {
        List<StockIndustry> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for industry constituents, got: {}", root.getNodeType());
                return result;
            }
            for (JsonNode node : root) {
                String stockCode = safeText(node, "代码");
                if (stockCode != null && !stockCode.isEmpty()) {
                    StockIndustry relation = new StockIndustry();
                    relation.setStockCode(stockCode);
                    relation.setIndustryCode(industryCode);
                    relation.setSnapDate(snapDate);
                    result.add(relation);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse industry constituents JSON for {}", industryCode, e);
            throw new RuntimeException("Failed to parse industry constituents: " + e.getMessage(), e);
        }
        return result;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }
}
