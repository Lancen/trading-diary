package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.mapper.StockDailyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

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
    private final ObjectMapper objectMapper;

    public StockDailyCleanseService(StockDailyMapper stockDailyMapper, ObjectMapper objectMapper) {
        this.stockDailyMapper = stockDailyMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Cleanse stock_zh_a_spot_em JSON into StockDaily entities (OHLCV data).
     * Uses the same JSON source as StockInfoCleanseService but extracts OHLC fields.
     *
     * @param rawJson   raw JSON from stock_zh_a_spot_em API
     * @param tradeDate the trade date
     * @return number of records inserted/updated
     */
    public int cleanse(String rawJson, LocalDate tradeDate) {
        List<StockDaily> entities = parseStockDailyList(rawJson, tradeDate);
        if (entities.isEmpty()) {
            log.warn("No stock daily records parsed for {}", tradeDate);
            return 0;
        }

        // Query existing records for this trade date
        List<StockDaily> existing = stockDailyMapper.selectList(
                new LambdaQueryWrapper<StockDaily>()
                        .eq(StockDaily::getTradeDate, tradeDate)
        );

        Map<String, StockDaily> existingByCode = existing.stream()
                .collect(Collectors.toMap(StockDaily::getStockCode, e -> e, (a, b) -> a));

        int count = 0;
        for (StockDaily entity : entities) {
            StockDaily existingEntity = existingByCode.get(entity.getStockCode());
            if (existingEntity != null) {
                entity.setId(existingEntity.getId());
                stockDailyMapper.updateById(entity);
            } else {
                try {
                    stockDailyMapper.insert(entity);
                } catch (DuplicateKeyException e) {
                    // Race condition: query and retry
                    StockDaily race = stockDailyMapper.selectOne(
                            new LambdaQueryWrapper<StockDaily>()
                                    .eq(StockDaily::getStockCode, entity.getStockCode())
                                    .eq(StockDaily::getTradeDate, tradeDate)
                    );
                    if (race != null) {
                        entity.setId(race.getId());
                        stockDailyMapper.updateById(entity);
                    } else {
                        throw e;
                    }
                }
            }
            count++;
        }

        log.info("StockDaily cleanse complete: {} records for {}", count, tradeDate);
        return count;
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
            throw new RuntimeException("Failed to parse stock daily data: " + e.getMessage(), e);
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
