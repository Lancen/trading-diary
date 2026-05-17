package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockDailyCleanseService {

    private static final Logger log = LoggerFactory.getLogger(StockDailyCleanseService.class);
    private final StockDailyMapper stockDailyMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public StockDailyCleanseService(StockDailyMapper stockDailyMapper, BatchSqlRunner batchSqlRunner,
                                     ObjectMapper objectMapper) {
        this.stockDailyMapper = stockDailyMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int cleanse(String rawJson, LocalDate tradeDate) {
        List<StockDaily> entities = parseStockDailyList(rawJson, tradeDate);
        if (entities.isEmpty()) {
            log.warn("No stock daily records parsed for {}", tradeDate);
            return 0;
        }

        List<StockDaily> existing = stockDailyMapper.selectList(
                new LambdaQueryWrapper<StockDaily>()
                        .eq(StockDaily::getTradeDate, tradeDate)
        );

        Map<String, StockDaily> existingByCode = existing.stream()
                .collect(Collectors.toMap(StockDaily::getStockCode, e -> e, (a, b) -> a));

        List<StockDaily> toInsert = new ArrayList<>();
        List<StockDaily> toUpdate = new ArrayList<>();

        for (StockDaily entity : entities) {
            StockDaily existingEntity = existingByCode.get(entity.getStockCode());
            if (existingEntity != null) {
                entity.setId(existingEntity.getId());
                toUpdate.add(entity);
            } else {
                toInsert.add(entity);
            }
        }

        int count = 0;
        if (!toInsert.isEmpty()) {
            count += batchSqlRunner.batchInsert(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            count += batchSqlRunner.batchUpdate(toUpdate);
        }

        log.info("StockDaily cleanse complete: {} records (insert={}, update={})",
                count, toInsert.size(), toUpdate.size());
        return count;
    }

    @Transactional
    public int cleanseHistBatch(List<String> rawJsonList, List<String> stockCodes) {
        List<StockDaily> allEntities = new ArrayList<>();
        for (int i = 0; i < rawJsonList.size(); i++) {
            allEntities.addAll(parseHistStockDailyListTx(rawJsonList.get(i), stockCodes.get(i)));
        }

        List<String> uniqueCodes = stockCodes.stream().distinct().toList();
        List<StockDaily> existing = stockDailyMapper.selectList(
                new LambdaQueryWrapper<StockDaily>().in(StockDaily::getStockCode, uniqueCodes));
        Map<String, StockDaily> existingMap = existing.stream()
                .collect(Collectors.toMap(
                        e -> e.getStockCode() + "|" + e.getTradeDate(), e -> e, (a, b) -> a));

        List<StockDaily> toInsert = new ArrayList<>();
        List<StockDaily> toUpdate = new ArrayList<>();
        for (StockDaily e : allEntities) {
            String key = e.getStockCode() + "|" + e.getTradeDate();
            StockDaily exist = existingMap.get(key);
            if (exist != null) {
                e.setId(exist.getId());
                toUpdate.add(e);
            } else {
                toInsert.add(e);
            }
        }

        int total = 0;
        if (!toInsert.isEmpty()) total += batchSqlRunner.batchInsert(toInsert);
        if (!toUpdate.isEmpty()) total += batchSqlRunner.batchUpdate(toUpdate);
        return total;
    }

    @Transactional
    public int cleanseHistJson(String rawJson, String stockCode) {
        List<StockDaily> entities = parseHistStockDailyListTx(rawJson, stockCode);
        if (entities.isEmpty()) {
            log.debug("No hist records parsed for {}", stockCode);
            return 0;
        }

        Map<LocalDate, StockDaily> existingMap = stockDailyMapper.selectList(
                new LambdaQueryWrapper<StockDaily>().eq(StockDaily::getStockCode, stockCode))
                .stream().collect(Collectors.toMap(StockDaily::getTradeDate, e -> e, (a, b) -> a));

        List<StockDaily> toInsert = new ArrayList<>();
        List<StockDaily> toUpdate = new ArrayList<>();
        for (StockDaily e : entities) {
            StockDaily exist = existingMap.get(e.getTradeDate());
            if (exist != null) {
                e.setId(exist.getId());
                toUpdate.add(e);
            } else {
                toInsert.add(e);
            }
        }

        int total = 0;
        if (!toInsert.isEmpty()) total += batchSqlRunner.batchInsert(toInsert);
        if (!toUpdate.isEmpty()) total += batchSqlRunner.batchUpdate(toUpdate);
        return total;
    }

    private List<StockDaily> parseHistStockDailyListTx(String rawJson, String stockCode) {
        List<StockDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for stock hist, got: {}", root.getNodeType());
                return result;
            }

            for (JsonNode node : root) {
                StockDaily daily = new StockDaily();
                daily.setStockCode(stockCode);
                String dateStr = safeText(node, "date");
                if (dateStr == null || dateStr.isEmpty()) continue;
                try {
                    daily.setTradeDate(LocalDate.parse(dateStr.substring(0, 10)));
                } catch (Exception e) {
                    log.debug("Failed to parse trade date: {}", dateStr);
                    continue;
                }
                daily.setOpen(safeDecimal(node, "open"));
                daily.setHigh(safeDecimal(node, "high"));
                daily.setLow(safeDecimal(node, "low"));
                daily.setClose(safeDecimal(node, "close"));
                daily.setVolume(safeLong(node, "volume"));
                daily.setAmount(safeDecimal(node, "amount"));
                result.add(daily);
            }
        } catch (Exception e) {
            log.error("Failed to parse stock hist JSON for {}", stockCode, e);
            throw new RuntimeException("解析个股历史日线数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private List<StockDaily> parseStockDailyList(String rawJson, LocalDate tradeDate) {
        List<StockDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for stock daily, got: {}", root.getNodeType());
                return result;
            }
            for (JsonNode node : root) {
                StockDaily daily = parseStockDaily(node, tradeDate);
                if (daily != null && daily.getStockCode() != null && !daily.getStockCode().isEmpty()) {
                    result.add(daily);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse stock daily JSON", e);
            throw new RuntimeException("解析股票日线数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private StockDaily parseStockDaily(JsonNode node, LocalDate tradeDate) {
        StockDaily daily = new StockDaily();
        daily.setStockCode(safeText(node, "代码"));
        daily.setTradeDate(tradeDate);
        daily.setOpen(safeDecimal(node, "今开"));
        daily.setHigh(safeDecimal(node, "最高"));
        daily.setLow(safeDecimal(node, "最低"));
        daily.setClose(safeDecimal(node, "最新价"));
        daily.setVolume(safeLong(node, "成交量"));
        daily.setAmount(safeDecimal(node, "成交额"));
        return daily;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }

    private BigDecimal safeDecimal(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        try {
            String text = fieldNode.asText();
            if (text == null || text.isEmpty() || "-".equals(text)) return null;
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeLong(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        try {
            String text = fieldNode.asText();
            if (text == null || text.isEmpty() || "-".equals(text)) return null;
            return Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
    }
}
