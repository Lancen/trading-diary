package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.ClassificationChangeLog;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.mapper.ClassificationChangeLogMapper;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConceptCleanseService {

    private static final Logger log = LoggerFactory.getLogger(ConceptCleanseService.class);

    private final ConceptMapper conceptMapper;
    private final StockConceptMapper stockConceptMapper;
    private final ClassificationChangeLogMapper classificationChangeLogMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public ConceptCleanseService(ConceptMapper conceptMapper,
                                 StockConceptMapper stockConceptMapper,
                                 ClassificationChangeLogMapper classificationChangeLogMapper,
                                 BatchSqlRunner batchSqlRunner,
                                 ObjectMapper objectMapper) {
        this.conceptMapper = conceptMapper;
        this.stockConceptMapper = stockConceptMapper;
        this.classificationChangeLogMapper = classificationChangeLogMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    /**
     * 清洗概念名称数据
     * <p>
     * 从原始JSON数据中解析概念名称信息，并将新增的概念保存到数据库。
     * 已存在的概念会被跳过，避免重复插入。
     * </p>
     *
     * @param rawJson 原始JSON数据字符串
     * @return 新插入的概念数量
     */
    public int cleanseNames(String rawJson) {
        List<Concept> concepts = parseConceptNames(rawJson);
        if (concepts.isEmpty()) {
            log.warn("No concept names parsed from response");
            return 0;
        }

        List<String> codes = concepts.stream().map(Concept::getCode).toList();
        Set<String> existingCodes = conceptMapper.selectList(
                new LambdaQueryWrapper<Concept>().in(Concept::getCode, codes))
                .stream().map(Concept::getCode).collect(Collectors.toSet());

        List<Concept> toInsert = concepts.stream()
                .filter(c -> !existingCodes.contains(c.getCode()))
                .toList();
        int count = batchSqlRunner.batchInsert(toInsert);

        log.info("Concept names cleanse complete: {} new concepts inserted", count);
        return count;
    }

    /**
     * 清洗概念成分股数据
     * <p>
     * 对比当日数据和数据库记录，检测成分股的增减变化。
     * 新增的成分股执行插入操作，移除的成分股执行逻辑删除操作。
     * 所有变化都会记录到分类变更日志中。
     * </p>
     *
     * @param rawJson 原始JSON数据字符串
     * @param conceptCode 概念代码
     * @param snapDate 快照日期
     * @return 变更数量（新增+移除）
     */
    public int cleanseCons(String rawJson, String conceptCode, LocalDate snapDate) {
        List<StockConcept> todayRelations = parseStockConceptList(rawJson, conceptCode, snapDate);
        if (todayRelations.isEmpty()) {
            log.info("No constituents parsed for concept {}", conceptCode);
            return 0;
        }

        Set<String> todayStockCodes = todayRelations.stream()
                .map(StockConcept::getStockCode)
                .collect(Collectors.toSet());

        List<StockConcept> dbRelations = stockConceptMapper.selectList(
                new LambdaQueryWrapper<StockConcept>()
                        .eq(StockConcept::getConceptCode, conceptCode)
                        .eq(StockConcept::getIsDeleted, false)
        );
        Set<String> dbStockCodes = dbRelations.stream()
                .map(StockConcept::getStockCode)
                .collect(Collectors.toSet());

        int changeCount = 0;
        List<StockConcept> toInsert = new ArrayList<>();
        List<StockConcept> toUpdate = new ArrayList<>();

        // ADD: stocks in today's data but not in DB
        for (StockConcept relation : todayRelations) {
            if (!dbStockCodes.contains(relation.getStockCode())) {
                toInsert.add(relation);
                insertChangeLog(relation.getStockCode(), "CONCEPT", conceptCode, "ADD", snapDate);
                changeCount++;
            }
        }
        batchSqlRunner.batchInsert(toInsert);

        // REMOVE: stocks in DB but not in today's data
        for (StockConcept dbRel : dbRelations) {
            if (!todayStockCodes.contains(dbRel.getStockCode())) {
                dbRel.setIsDeleted(true);
                toUpdate.add(dbRel);
                insertChangeLog(dbRel.getStockCode(), "CONCEPT", conceptCode, "REMOVE", snapDate);
                changeCount++;
            }
        }
        batchSqlRunner.batchUpdate(toUpdate);

        log.info("Concept cons cleanse complete: {} changes for {}", changeCount, conceptCode);
        return changeCount;
    }

    private void insertChangeLog(String stockCode, String type, String sectorCode, String action, LocalDate snapDate) {
        ClassificationChangeLog logEntry = new ClassificationChangeLog();
        logEntry.setStockCode(stockCode);
        logEntry.setClassificationType(type);
        logEntry.setSectorCode(sectorCode);
        logEntry.setAction(action);
        logEntry.setSnapDate(snapDate);
        classificationChangeLogMapper.insert(logEntry);
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
                String code = safeText(node, "code");
                String name = safeText(node, "name");
                if (code != null && !code.isEmpty() && name != null && !name.isEmpty()) {
                    Concept concept = new Concept();
                    concept.setCode(code);
                    concept.setName(name);
                    result.add(concept);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse concept names JSON", e);
            throw new RuntimeException("解析概念名称失败: " + e.getMessage(), e);
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
            throw new RuntimeException("解析概念成分股失败: " + e.getMessage(), e);
        }
        return result;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }
}