package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.CollectionConstants;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.service.collection.ConstituentScrapeService;
import com.tradingdiary.service.collection.SystemConfigService;
import com.tradingdiary.util.BatchSqlRunner;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ConstituentScrapeServiceImpl implements ConstituentScrapeService {

    private static final Logger log = LoggerFactory.getLogger(ConstituentScrapeServiceImpl.class);

    private static final int SCRAPE_TIMEOUT_MINUTES = 5;

    private final Map<String, Map<String, Object>> scrapeStatusMap = new ConcurrentHashMap<>();

    private final StockIndustryMapper stockIndustryMapper;
    private final StockConceptMapper stockConceptMapper;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;
    private final BatchSqlRunner batchSqlRunner;
    private final String scriptPath;
    private final Path dataDir;

    public ConstituentScrapeServiceImpl(StockIndustryMapper stockIndustryMapper,
                                         StockConceptMapper stockConceptMapper,
                                         ObjectMapper objectMapper,
                                         SystemConfigService systemConfigService,
                                         BatchSqlRunner batchSqlRunner,
                                         @Value("${app.collection.scripts-dir:scripts}") String scriptsDir,
                                         @Value("${app.collection.constituents-dir:data/constituents}") String dataDir) {
        this.stockIndustryMapper = stockIndustryMapper;
        this.stockConceptMapper = stockConceptMapper;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
        this.batchSqlRunner = batchSqlRunner;
        this.scriptPath = Paths.get(scriptsDir, "scrape_ths_constituents.py").toString();
        this.dataDir = Paths.get(dataDir);
    }

    @Override
    public void startAsyncScrape(String boardType, String code) {
        String key = boardType + ":" + code;
        if (scrapeStatusMap.containsKey(key)) {
            Map<String, Object> current = scrapeStatusMap.get(key);
            if ("scraping".equals(current.get("status"))) {
                return;
            }
        }

        scrapeStatusMap.put(key, Map.of("status", "scraping", "boardType", boardType, "code", code));

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> result = scrapeAndImport(boardType, code);
                scrapeStatusMap.put(key, result);
            } catch (Exception e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("status", "failed");
                fail.put("error", e.getMessage());
                fail.put("boardType", boardType);
                fail.put("code", code);
                scrapeStatusMap.put(key, fail);
            }
        });
    }

    @Override
    public Map<String, Object> getScrapeStatus(String boardType, String code) {
        String key = boardType + ":" + code;
        return scrapeStatusMap.getOrDefault(key, Map.of("status", "idle", "boardType", boardType, "code", code));
    }

    @Override
    public Map<String, Object> scrapeAndImport(String boardType, String code) {
        if (!"industry".equals(boardType) && !"concept".equals(boardType)) {
            throw new IllegalArgumentException("无效的板块类型: " + boardType + "，仅支持 industry/concept");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("板块代码不能为空");
        }

        String json = runScraper(boardType, code);
        if (json == null || json.isBlank()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "failed");
            result.put("error", "抓取脚本未返回数据");
            result.put("boardType", boardType);
            result.put("code", code);
            return result;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Failed to parse scraper output for {}/{}", boardType, code, e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "failed");
            result.put("error", "JSON 解析失败: " + e.getMessage());
            result.put("boardType", boardType);
            result.put("code", code);
            return result;
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
            String thsCookie = systemConfigService.getThsCookie();

            List<String> command = new ArrayList<>();
            command.add("python3");
            command.add(scriptPath);
            command.add("--type");
            command.add(boardType);
            command.add("--code");
            command.add(code);
            if (thsCookie != null && !thsCookie.isBlank()) {
                command.add("--cookie");
                command.add(thsCookie);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
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

    @Transactional
    protected int importToDatabase(JsonNode root, String boardType, LocalDate snapDate) {
        String key = "industry".equals(boardType) ? "industries" : "concepts";
        if (!root.has(key)) {
            return 0;
        }

        List<String> allStockCodes = new ArrayList<>();
        String boardCode = null;
        for (JsonNode board : root.get(key)) {
            boardCode = board.get("code").asText();
            if (board.has("stocks")) {
                for (JsonNode stock : board.get("stocks")) {
                    String stockCode = stock.asText();
                    if (stockCode != null && !stockCode.isEmpty()) {
                        allStockCodes.add(stockCode);
                    }
                }
            }
        }

        if (allStockCodes.isEmpty() || boardCode == null) {
            return 0;
        }

        if ("industry".equals(boardType)) {
            return batchUpsertStockIndustry(allStockCodes, boardCode, snapDate);
        } else {
            return batchUpsertStockConcept(allStockCodes, boardCode, snapDate);
        }
    }

    private int batchUpsertStockIndustry(List<String> stockCodes, String industryCode, LocalDate snapDate) {
        List<StockIndustry> existing = stockIndustryMapper.selectList(
                new LambdaQueryWrapper<StockIndustry>()
                        .eq(StockIndustry::getIndustryCode, industryCode)
                        .eq(StockIndustry::getIsDeleted, false)
        );

        Set<String> newStockSet = new HashSet<>(stockCodes);
        Set<String> existingKeys = new HashSet<>();

        List<StockIndustry> toInsert = new ArrayList<>();
        List<StockIndustry> toUpdate = new ArrayList<>();
        List<StockIndustry> toDelete = new ArrayList<>();

        for (StockIndustry si : existing) {
            existingKeys.add(si.getStockCode() + ":" + si.getIndustryCode());
            if (!newStockSet.contains(si.getStockCode())) {
                si.setIsDeleted(true);
                toDelete.add(si);
            }
        }

        for (String stockCode : stockCodes) {
            String compositeKey = stockCode + ":" + industryCode;
            if (existingKeys.contains(compositeKey)) {
                for (StockIndustry ex : existing) {
                    if (ex.getStockCode().equals(stockCode) && ex.getIndustryCode().equals(industryCode)) {
                        ex.setSnapDate(snapDate);
                        toUpdate.add(ex);
                        break;
                    }
                }
            } else {
                StockIndustry si = new StockIndustry();
                si.setStockCode(stockCode);
                si.setIndustryCode(industryCode);
                si.setSnapDate(snapDate);
                si.setIsDeleted(false);
                toInsert.add(si);
            }
        }

        if (!toDelete.isEmpty()) {
            batchSqlRunner.batchUpdate(toDelete);
            log.info("Batch soft-deleted {} stock_industry records for industry {}", toDelete.size(), industryCode);
        }
        if (!toInsert.isEmpty()) {
            batchSqlRunner.batchInsert(toInsert);
            log.info("Batch inserted {} stock_industry records for industry {}", toInsert.size(), industryCode);
        }
        if (!toUpdate.isEmpty()) {
            batchSqlRunner.batchUpdate(toUpdate);
            log.info("Batch updated {} stock_industry records for industry {}", toUpdate.size(), industryCode);
        }

        return newStockSet.size();
    }

    private int batchUpsertStockConcept(List<String> stockCodes, String conceptCode, LocalDate snapDate) {
        List<StockConcept> existing = stockConceptMapper.selectList(
                new LambdaQueryWrapper<StockConcept>()
                        .eq(StockConcept::getConceptCode, conceptCode)
                        .eq(StockConcept::getIsDeleted, false)
        );

        Set<String> newStockSet = new HashSet<>(stockCodes);
        Set<String> existingKeys = new HashSet<>();

        List<StockConcept> toInsert = new ArrayList<>();
        List<StockConcept> toUpdate = new ArrayList<>();
        List<StockConcept> toDelete = new ArrayList<>();

        for (StockConcept sc : existing) {
            existingKeys.add(sc.getStockCode() + ":" + sc.getConceptCode());
            if (!newStockSet.contains(sc.getStockCode())) {
                sc.setIsDeleted(true);
                toDelete.add(sc);
            }
        }

        for (String stockCode : stockCodes) {
            String compositeKey = stockCode + ":" + conceptCode;
            if (existingKeys.contains(compositeKey)) {
                for (StockConcept ex : existing) {
                    if (ex.getStockCode().equals(stockCode) && ex.getConceptCode().equals(conceptCode)) {
                        ex.setSnapDate(snapDate);
                        toUpdate.add(ex);
                        break;
                    }
                }
            } else {
                StockConcept sc = new StockConcept();
                sc.setStockCode(stockCode);
                sc.setConceptCode(conceptCode);
                sc.setSnapDate(snapDate);
                sc.setIsDeleted(false);
                toInsert.add(sc);
            }
        }

        if (!toDelete.isEmpty()) {
            batchSqlRunner.batchUpdate(toDelete);
            log.info("Batch soft-deleted {} stock_concept records for concept {}", toDelete.size(), conceptCode);
        }
        if (!toInsert.isEmpty()) {
            batchSqlRunner.batchInsert(toInsert);
            log.info("Batch inserted {} stock_concept records for concept {}", toInsert.size(), conceptCode);
        }
        if (!toUpdate.isEmpty()) {
            batchSqlRunner.batchUpdate(toUpdate);
            log.info("Batch updated {} stock_concept records for concept {}", toUpdate.size(), conceptCode);
        }

        return newStockSet.size();
    }
}