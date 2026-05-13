package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.StockConceptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConceptCleanseService {

    private static final Logger log = LoggerFactory.getLogger(ConceptCleanseService.class);

    private final ConceptMapper conceptMapper;
    private final StockConceptMapper stockConceptMapper;
    private final ObjectMapper objectMapper;

    public ConceptCleanseService(ConceptMapper conceptMapper,
                                 StockConceptMapper stockConceptMapper,
                                 ObjectMapper objectMapper) {
        this.conceptMapper = conceptMapper;
        this.stockConceptMapper = stockConceptMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Cleanse concept name list from stock_board_concept_name_em response.
     * Saves new concept names to the concept table (simple INSERT — change detection in Phase 6).
     *
     * @param rawJson raw JSON from stock_board_concept_name_em API
     * @return number of new concepts inserted
     */
    public int cleanseNames(String rawJson) {
        List<Concept> concepts = parseConceptNames(rawJson);
        if (concepts.isEmpty()) {
            log.warn("No concept names parsed from response");
            return 0;
        }

        int count = 0;
        for (Concept concept : concepts) {
            Long exists = conceptMapper.selectCount(
                    new LambdaQueryWrapper<Concept>()
                            .eq(Concept::getCode, concept.getCode())
            );
            if (exists == 0) {
                conceptMapper.insert(concept);
                count++;
            }
        }

        log.info("Concept names cleanse complete: {} new concepts inserted", count);
        return count;
    }

    /**
     * Cleanse constituent list for ONE concept from stock_board_concept_cons_em response.
     * For now, simple saveOrUpdate. Change detection will be added in Phase 6.
     *
     * @param rawJson     raw JSON from stock_board_concept_cons_em API
     * @param conceptCode the concept board code
     * @param snapDate    the snapshot date
     * @return number of stock-concept relations saved
     */
    public int cleanseCons(String rawJson, String conceptCode, LocalDate snapDate) {
        List<StockConcept> relations = parseStockConceptList(rawJson, conceptCode, snapDate);
        if (relations.isEmpty()) {
            log.info("No constituents parsed for concept {}", conceptCode);
            return 0;
        }

        int count = 0;
        for (StockConcept relation : relations) {
            StockConcept existing = stockConceptMapper.selectOne(
                    new LambdaQueryWrapper<StockConcept>()
                            .eq(StockConcept::getStockCode, relation.getStockCode())
                            .eq(StockConcept::getConceptCode, relation.getConceptCode())
            );
            if (existing != null) {
                relation.setId(existing.getId());
                stockConceptMapper.updateById(relation);
            } else {
                stockConceptMapper.insert(relation);
            }
            count++;
        }

        log.info("Concept cons cleanse complete: {} constituents for {}", count, conceptCode);
        return count;
    }

    private List<Concept> parseConceptNames(String rawJson) {
        List<Concept> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for concept names, got: {}", root.getNodeType());
                return result;
            }
            for (JsonNode node : root) {
                String code = safeText(node, "板块代码");
                String name = safeText(node, "板块名称");
                if (code != null && !code.isEmpty() && name != null && !name.isEmpty()) {
                    Concept concept = new Concept();
                    concept.setCode(code);
                    concept.setName(name);
                    result.add(concept);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse concept names JSON", e);
            throw new RuntimeException("Failed to parse concept names: " + e.getMessage(), e);
        }
        return result;
    }

    private List<StockConcept> parseStockConceptList(String rawJson, String conceptCode, LocalDate snapDate) {
        List<StockConcept> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for concept constituents, got: {}", root.getNodeType());
                return result;
            }
            for (JsonNode node : root) {
                String stockCode = safeText(node, "代码");
                if (stockCode != null && !stockCode.isEmpty()) {
                    StockConcept relation = new StockConcept();
                    relation.setStockCode(stockCode);
                    relation.setConceptCode(conceptCode);
                    relation.setSnapDate(snapDate);
                    result.add(relation);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse concept constituents JSON for {}", conceptCode, e);
            throw new RuntimeException("Failed to parse concept constituents: " + e.getMessage(), e);
        }
        return result;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }
}
