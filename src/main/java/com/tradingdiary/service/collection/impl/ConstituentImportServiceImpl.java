package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.service.collection.ConstituentImportService;
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
public class ConstituentImportServiceImpl implements ConstituentImportService {

    private static final Logger log = LoggerFactory.getLogger(ConstituentImportServiceImpl.class);

    private final StockIndustryMapper stockIndustryMapper;
    private final StockConceptMapper stockConceptMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public ConstituentImportServiceImpl(StockIndustryMapper stockIndustryMapper,
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

    @Override
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

                            boolean imported = false;
                            if (info.get("fetchedDate") != null) {
                                try {
                                    LocalDate snapDate = LocalDate.parse((String) info.get("fetchedDate"));
                                    imported = stockIndustryMapper.selectCount(
                                            new LambdaQueryWrapper<StockIndustry>()
                                                    .eq(StockIndustry::getSnapDate, snapDate)
                                                    .eq(StockIndustry::getIsDeleted, false)
                                                    .last("LIMIT 1")
                                    ) > 0;
                                } catch (Exception e) {
                                    log.debug("Failed to check import status for {}: {}", p.getFileName(), e.getMessage());
                                }
                            }
                            info.put("imported", imported);
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

    @Override
    @Transactional
    public Map<String, Object> importFromFile(String filename) {
        Path file = dataDir.resolve(filename).normalize();
        if (!file.startsWith(dataDir)) {
            throw new IllegalArgumentException("非法文件路径: " + filename);
        }
        if (!Files.exists(file)) {
            return Map.of("status", "failed", "error", "文件不存在: " + filename);
        }

        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            int industryCount = 0;
            int conceptCount = 0;

            String fetchedDate = root.has("fetched_date") ? root.get("fetched_date").asText() : null;
            LocalDate snapDate = fetchedDate != null ? LocalDate.parse(fetchedDate) : LocalDate.now();

            if (root.has("industries")) {
                for (JsonNode ind : root.get("industries")) {
                    String industryCode = ind.get("code").asText();
                    if (ind.has("stocks")) {
                        for (JsonNode stock : ind.get("stocks")) {
                            String stockCode = stock.asText();
                            if (stockCode == null || stockCode.isEmpty()) continue;
                            industryCount += upsertStockIndustry(stockCode, industryCode, snapDate);
                        }
                    }
                }
            }

            if (root.has("concepts")) {
                for (JsonNode con : root.get("concepts")) {
                    String conceptCode = con.get("code").asText();
                    if (con.has("stocks")) {
                        for (JsonNode stock : con.get("stocks")) {
                            String stockCode = stock.asText();
                            if (stockCode == null || stockCode.isEmpty()) continue;
                            conceptCount += upsertStockConcept(stockCode, conceptCode, snapDate);
                        }
                    }
                }
            }

            log.info("Constituent import complete: {} industries, {} concepts from {}", industryCount, conceptCount, filename);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("industryRelations", industryCount);
            result.put("conceptRelations", conceptCount);
            return result;
        } catch (Exception e) {
            log.error("Failed to import constituent file: {}", filename, e);
            return Map.of("status", "failed", "error", e.getMessage());
        }
    }

    private int upsertStockIndustry(String stockCode, String industryCode, LocalDate snapDate) {
        StockIndustry existing = stockIndustryMapper.selectOne(
                new LambdaQueryWrapper<StockIndustry>()
                        .eq(StockIndustry::getStockCode, stockCode)
                        .eq(StockIndustry::getIndustryCode, industryCode)
                        .eq(StockIndustry::getIsDeleted, false)
        );

        if (existing != null) {
            existing.setSnapDate(snapDate);
            return stockIndustryMapper.updateById(existing);
        } else {
            StockIndustry si = new StockIndustry();
            si.setStockCode(stockCode);
            si.setIndustryCode(industryCode);
            si.setSnapDate(snapDate);
            stockIndustryMapper.insert(si);
            return 1;
        }
    }

    private int upsertStockConcept(String stockCode, String conceptCode, LocalDate snapDate) {
        StockConcept existing = stockConceptMapper.selectOne(
                new LambdaQueryWrapper<StockConcept>()
                        .eq(StockConcept::getStockCode, stockCode)
                        .eq(StockConcept::getConceptCode, conceptCode)
                        .eq(StockConcept::getIsDeleted, false)
        );

        if (existing != null) {
            existing.setSnapDate(snapDate);
            return stockConceptMapper.updateById(existing);
        } else {
            StockConcept sc = new StockConcept();
            sc.setStockCode(stockCode);
            sc.setConceptCode(conceptCode);
            sc.setSnapDate(snapDate);
            stockConceptMapper.insert(sc);
            return 1;
        }
    }
}
