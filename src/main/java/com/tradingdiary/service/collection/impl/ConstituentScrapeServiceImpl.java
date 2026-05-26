package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.service.collection.ConstituentScrapeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ConstituentScrapeServiceImpl implements ConstituentScrapeService {

    private static final Logger log = LoggerFactory.getLogger(ConstituentScrapeServiceImpl.class);

    private static final int SCRAPE_TIMEOUT_MINUTES = 5;

    private final StockIndustryMapper stockIndustryMapper;
    private final StockConceptMapper stockConceptMapper;
    private final ObjectMapper objectMapper;
    private final String scriptPath;
    private final Path dataDir;

    public ConstituentScrapeServiceImpl(StockIndustryMapper stockIndustryMapper,
                                         StockConceptMapper stockConceptMapper,
                                         ObjectMapper objectMapper,
                                         @Value("${app.collection.scripts-dir:scripts}") String scriptsDir,
                                         @Value("${app.collection.constituents-dir:data/constituents}") String dataDir) {
        this.stockIndustryMapper = stockIndustryMapper;
        this.stockConceptMapper = stockConceptMapper;
        this.objectMapper = objectMapper;
        this.scriptPath = Paths.get(scriptsDir, "scrape_ths_constituents.py").toString();
        this.dataDir = Paths.get(dataDir);
    }

    @Override
    @Transactional
    public Map<String, Object> scrapeAndImport(String boardType, String code) {
        if (!"industry".equals(boardType) && !"concept".equals(boardType)) {
            throw new IllegalArgumentException("无效的板块类型: " + boardType + "，仅支持 industry/concept");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("板块代码不能为空");
        }

        String json = runScraper(boardType, code);
        if (json == null || json.isBlank()) {
            return Map.of("status", "failed", "error", "抓取脚本未返回数据");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Failed to parse scraper output for {}/{}", boardType, code, e);
            return Map.of("status", "failed", "error", "JSON 解析失败: " + e.getMessage());
        }

        String fetchedDate = root.has("fetched_date") ? root.get("fetched_date").asText() : null;
        LocalDate snapDate = fetchedDate != null ? LocalDate.parse(fetchedDate) : LocalDate.now();

        saveToLocal(root, boardType, code, snapDate);

        int relationCount = importToDatabase(root, boardType, snapDate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("boardType", boardType);
        result.put("code", code);
        result.put("snapDate", snapDate.toString());
        result.put("relationCount", relationCount);
        return result;
    }

    private String runScraper(String boardType, String code) {
        log.info("Running scraper: type={}, code={}", boardType, code);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", scriptPath,
                    "--type", boardType,
                    "--code", code
            );
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONUNBUFFERED", "1");

            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(SCRAPE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("Scraper timed out for {}/{}", boardType, code);
                return null;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Scraper exited with code {} for {}/{}: {}", exitCode, boardType, code, output);
                return null;
            }

            String jsonLine = null;
            for (String line : output.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    jsonLine = trimmed;
                    break;
                }
            }

            if (jsonLine == null) {
                log.error("No JSON found in scraper output for {}/{}", boardType, code);
                return null;
            }

            return jsonLine;
        } catch (Exception e) {
            log.error("Failed to run scraper for {}/{}", boardType, code, e);
            return null;
        }
    }

    private void saveToLocal(JsonNode root, String boardType, String code, LocalDate snapDate) {
        try {
            Path dateDir = dataDir.resolve(snapDate.toString());
            Files.createDirectories(dateDir);
            Path file = dateDir.resolve(code + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
            log.info("Saved constituent data to {}", file);
        } catch (Exception e) {
            log.warn("Failed to save constituent data locally for {}/{}", boardType, code, e);
        }
    }

    private int importToDatabase(JsonNode root, String boardType, LocalDate snapDate) {
        String key = "industry".equals(boardType) ? "industries" : "concepts";
        int totalRelations = 0;

        if (root.has(key)) {
            for (JsonNode board : root.get(key)) {
                String boardCode = board.get("code").asText();
                if (board.has("stocks")) {
                    for (JsonNode stock : board.get("stocks")) {
                        String stockCode = stock.asText();
                        if (stockCode == null || stockCode.isEmpty()) continue;
                        if ("industry".equals(boardType)) {
                            totalRelations += upsertStockIndustry(stockCode, boardCode, snapDate);
                        } else {
                            totalRelations += upsertStockConcept(stockCode, boardCode, snapDate);
                        }
                    }
                }
            }
        }

        log.info("Imported {} constituent relations for {}/{}", totalRelations, boardType, root.get(key).get(0).get("code").asText());
        return totalRelations;
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
            stockIndustryMapper.updateById(existing);
            return 0;
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
            stockConceptMapper.updateById(existing);
            return 0;
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
