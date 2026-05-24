package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.ClassificationChangeLog;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.ClassificationChangeLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.service.collection.IndustryCleanseService;
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
public class IndustryCleanseServiceImpl implements IndustryCleanseService {

    private static final Logger log = LoggerFactory.getLogger(IndustryCleanseServiceImpl.class);

    private final IndustryMapper industryMapper;
    private final StockIndustryMapper stockIndustryMapper;
    private final ClassificationChangeLogMapper classificationChangeLogMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public IndustryCleanseServiceImpl(IndustryMapper industryMapper,
                                       StockIndustryMapper stockIndustryMapper,
                                       ClassificationChangeLogMapper classificationChangeLogMapper,
                                       BatchSqlRunner batchSqlRunner,
                                       ObjectMapper objectMapper) {
        this.industryMapper = industryMapper;
        this.stockIndustryMapper = stockIndustryMapper;
        this.classificationChangeLogMapper = classificationChangeLogMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    public int cleanseNames(String rawJson) {
        List<Industry> industries = parseIndustryNames(rawJson);
        if (industries.isEmpty()) {
            log.warn("No industry names parsed from response");
            return 0;
        }

        List<String> codes = industries.stream().map(Industry::getCode).toList();
        Set<String> existingCodes = industryMapper.selectList(
                new LambdaQueryWrapper<Industry>().in(Industry::getCode, codes))
                .stream().map(Industry::getCode).collect(Collectors.toSet());

        List<Industry> toInsert = industries.stream()
                .filter(i -> !existingCodes.contains(i.getCode()))
                .toList();
        int count = batchSqlRunner.batchInsert(toInsert);

        log.info("Industry names cleanse complete: {} new industries inserted", count);
        return count;
    }

    @Override
    public int cleanseCons(String rawJson, String industryCode, LocalDate snapDate) {
        List<StockIndustry> todayRelations = parseStockIndustryList(rawJson, industryCode, snapDate);
        if (todayRelations.isEmpty()) {
            log.info("No constituents parsed for industry {}", industryCode);
            return 0;
        }

        Set<String> todayStockCodes = todayRelations.stream()
                .map(StockIndustry::getStockCode)
                .collect(Collectors.toSet());

        List<StockIndustry> dbRelations = stockIndustryMapper.selectList(
                new LambdaQueryWrapper<StockIndustry>()
                        .eq(StockIndustry::getIndustryCode, industryCode)
                        .eq(StockIndustry::getIsDeleted, false)
        );
        Set<String> dbStockCodes = dbRelations.stream()
                .map(StockIndustry::getStockCode)
                .collect(Collectors.toSet());

        int changeCount = 0;
        List<StockIndustry> toInsert = new ArrayList<>();
        List<StockIndustry> toUpdate = new ArrayList<>();

        for (StockIndustry relation : todayRelations) {
            if (!dbStockCodes.contains(relation.getStockCode())) {
                toInsert.add(relation);
                insertChangeLog(relation.getStockCode(), "INDUSTRY", industryCode, "ADD", snapDate);
                changeCount++;
            }
        }
        batchSqlRunner.batchInsert(toInsert);

        for (StockIndustry dbRel : dbRelations) {
            if (!todayStockCodes.contains(dbRel.getStockCode())) {
                dbRel.setIsDeleted(true);
                toUpdate.add(dbRel);
                insertChangeLog(dbRel.getStockCode(), "INDUSTRY", industryCode, "REMOVE", snapDate);
                changeCount++;
            }
        }
        batchSqlRunner.batchUpdate(toUpdate);

        log.info("Industry cons cleanse complete: {} changes for {}", changeCount, industryCode);
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

    private List<Industry> parseIndustryNames(String rawJson) {
        List<Industry> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for industry names, got: {}", root.getNodeType());
                return result;
            }
            for (JsonNode node : root) {
                String code = safeText(node, "code");
                String name = safeText(node, "name");
                if (code != null && !code.isEmpty() && name != null && !name.isEmpty()) {
                    Industry industry = new Industry();
                    industry.setCode(code);
                    industry.setName(name);
                    result.add(industry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse industry names JSON", e);
            throw new RuntimeException("解析行业名称失败: " + e.getMessage(), e);
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
            throw new RuntimeException("解析行业成分股失败: " + e.getMessage(), e);
        }
        return result;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }
}
