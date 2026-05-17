package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConstituentImportService {

    private static final Logger log = LoggerFactory.getLogger(ConstituentImportService.class);

    private final StockIndustryMapper stockIndustryMapper;
    private final StockConceptMapper stockConceptMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public ConstituentImportService(StockIndustryMapper stockIndustryMapper,
                                     StockConceptMapper stockConceptMapper,
                                     BatchSqlRunner batchSqlRunner,
                                     ObjectMapper objectMapper,
                                     @Value("${app.collection.constituents-dir:data/constituents}") String dataDir) {
        this.stockIndustryMapper = stockIndustryMapper;
        this.stockConceptMapper = stockConceptMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
        this.dataDir = Paths.get(dataDir);
    }

    /**
     * 列出可用的成分股数据文件。
     */
    public List<Map<String, Object>> listFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        if (!Files.exists(dataDir)) return files;

        try (Stream<Path> stream = Files.list(dataDir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        Map<String, Object> info = new LinkedHashMap<>();
                        info.put("filename", p.getFileName().toString());
                        try {
                            info.put("size", Files.size(p));
                            info.put("lastModified", Files.getLastModifiedTime(p).toInstant().toString());
                            // 读取 JSON 元数据
                            JsonNode root = objectMapper.readTree(p.toFile());
                            info.put("fetchedDate", root.has("fetched_date") ? root.get("fetched_date").asText() : null);
                            info.put("industryCount", root.has("industries") ? root.get("industries").size() : 0);
                            info.put("conceptCount", root.has("concepts") ? root.get("concepts").size() : 0);
                            int totalStocks = 0;
                            if (root.has("industries")) {
                                for (JsonNode ind : root.get("industries")) {
                                    totalStocks += ind.has("stocks") ? ind.get("stocks").size() : 0;
                                }
                            }
                            if (root.has("concepts")) {
                                for (JsonNode con : root.get("concepts")) {
                                    totalStocks += con.has("stocks") ? con.get("stocks").size() : 0;
                                }
                            }
                            info.put("totalRelations", totalStocks);
                        } catch (Exception e) {
                            info.put("error", e.getMessage());
                        }
                        files.add(info);
                    });
        } catch (IOException e) {
            log.error("Failed to list constituent files", e);
        }
        return files;
    }

    /**
     * 从指定文件导入行业和概念成分股数据。
     */
    public Map<String, Object> importFromFile(String filename) {
        Path file = dataDir.resolve(filename).normalize();
        if (!file.startsWith(dataDir)) {
            throw new IllegalArgumentException("非法文件路径: " + filename);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", filename);

        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            LocalDate snapDate = root.has("fetched_date")
                    ? LocalDate.parse(root.get("fetched_date").asText())
                    : LocalDate.now();

            int industryRelCount = importIndustries(root, snapDate);
            int conceptRelCount = importConcepts(root, snapDate);

            result.put("snapDate", snapDate.toString());
            result.put("industryRelations", industryRelCount);
            result.put("conceptRelations", conceptRelCount);
            result.put("status", "success");

            log.info("Constituent import complete: {} industry + {} concept relations from {}",
                    industryRelCount, conceptRelCount, filename);
        } catch (Exception e) {
            log.error("Failed to import constituents from {}", filename, e);
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Transactional
    private int importIndustries(JsonNode root, LocalDate snapDate) {
        JsonNode industries = root.get("industries");
        if (industries == null || !industries.isArray()) return 0;

        List<StockIndustry> allRelations = new ArrayList<>();
        for (JsonNode ind : industries) {
            String industryCode = ind.get("code").asText();
            JsonNode stocks = ind.get("stocks");
            if (stocks == null || !stocks.isArray()) continue;

            for (JsonNode stock : stocks) {
                StockIndustry si = new StockIndustry();
                si.setStockCode(stock.asText());
                si.setIndustryCode(industryCode);
                si.setSnapDate(snapDate);
                allRelations.add(si);
            }
        }

        return batchUpsertStockIndustry(allRelations, snapDate);
    }

    @Transactional
    private int importConcepts(JsonNode root, LocalDate snapDate) {
        JsonNode concepts = root.get("concepts");
        if (concepts == null || !concepts.isArray()) return 0;

        List<StockConcept> allRelations = new ArrayList<>();
        for (JsonNode con : concepts) {
            String conceptCode = con.get("code").asText();
            JsonNode stocks = con.get("stocks");
            if (stocks == null || !stocks.isArray()) continue;

            for (JsonNode stock : stocks) {
                StockConcept sc = new StockConcept();
                sc.setStockCode(stock.asText());
                sc.setConceptCode(conceptCode);
                sc.setSnapDate(snapDate);
                allRelations.add(sc);
            }
        }

        return batchUpsertStockConcept(allRelations, snapDate);
    }

    private int batchUpsertStockIndustry(List<StockIndustry> relations, LocalDate snapDate) {
        if (relations.isEmpty()) return 0;

        Set<String> stockCodes = relations.stream().map(StockIndustry::getStockCode).collect(Collectors.toSet());
        Map<String, StockIndustry> existingMap = stockIndustryMapper.selectList(
                new LambdaQueryWrapper<StockIndustry>()
                        .in(StockIndustry::getStockCode, stockCodes)
                        .eq(StockIndustry::getIsDeleted, false)
        ).stream().collect(Collectors.toMap(
                e -> e.getStockCode() + "|" + e.getIndustryCode(), e -> e, (a, b) -> a));

        List<StockIndustry> toInsert = new ArrayList<>();
        List<StockIndustry> toUpdate = new ArrayList<>();

        for (StockIndustry si : relations) {
            String key = si.getStockCode() + "|" + si.getIndustryCode();
            StockIndustry existing = existingMap.get(key);
            if (existing != null) {
                existing.setSnapDate(snapDate);
                toUpdate.add(existing);
            } else {
                toInsert.add(si);
            }
        }

        int count = 0;
        if (!toInsert.isEmpty()) count += batchSqlRunner.batchInsert(toInsert);
        if (!toUpdate.isEmpty()) count += batchSqlRunner.batchUpdate(toUpdate);
        return count;
    }

    private int batchUpsertStockConcept(List<StockConcept> relations, LocalDate snapDate) {
        if (relations.isEmpty()) return 0;

        Set<String> stockCodes = relations.stream().map(StockConcept::getStockCode).collect(Collectors.toSet());
        Map<String, StockConcept> existingMap = stockConceptMapper.selectList(
                new LambdaQueryWrapper<StockConcept>()
                        .in(StockConcept::getStockCode, stockCodes)
                        .eq(StockConcept::getIsDeleted, false)
        ).stream().collect(Collectors.toMap(
                e -> e.getStockCode() + "|" + e.getConceptCode(), e -> e, (a, b) -> a));

        List<StockConcept> toInsert = new ArrayList<>();
        List<StockConcept> toUpdate = new ArrayList<>();

        for (StockConcept sc : relations) {
            String key = sc.getStockCode() + "|" + sc.getConceptCode();
            StockConcept existing = existingMap.get(key);
            if (existing != null) {
                existing.setSnapDate(snapDate);
                toUpdate.add(existing);
            } else {
                toInsert.add(sc);
            }
        }

        int count = 0;
        if (!toInsert.isEmpty()) count += batchSqlRunner.batchInsert(toInsert);
        if (!toUpdate.isEmpty()) count += batchSqlRunner.batchUpdate(toUpdate);
        return count;
    }
}
