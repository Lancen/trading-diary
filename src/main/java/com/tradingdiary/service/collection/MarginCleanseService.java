package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.entity.MarginStock;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginStockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarginCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginCleanseService.class);

    private final MarginDailyMapper marginDailyMapper;
    private final MarginStockMapper marginStockMapper;
    private final ObjectMapper objectMapper;

    public MarginCleanseService(MarginDailyMapper marginDailyMapper,
                                MarginStockMapper marginStockMapper,
                                ObjectMapper objectMapper) {
        this.marginDailyMapper = marginDailyMapper;
        this.marginStockMapper = marginStockMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Cleanse margin detail JSON (from stock_margin_detail_sse or stock_margin_detail_szse)
     * and save to margin_daily + margin_stock tables.
     *
     * @param rawJson   raw JSON from margin detail API
     * @param exchange  "SSE" or "SZSE"
     * @param tradeDate the trade date
     * @return number of margin_daily records saved
     */
    public int cleanse(String rawJson, String exchange, LocalDate tradeDate) {
        List<MarginDaily> dailyList = parseMarginDailyList(rawJson, exchange, tradeDate);
        if (dailyList.isEmpty()) {
            log.warn("No margin daily records parsed for {} on {}", exchange, tradeDate);
            return 0;
        }

        // Save margin_daily records
        int dailyCount = saveMarginDailyList(dailyList, exchange, tradeDate);

        // Extract unique stock codes and save to margin_stock
        saveMarginStocks(dailyList, exchange, tradeDate);

        log.info("Margin cleanse complete: {} margin_daily records for {} on {}",
                dailyCount, exchange, tradeDate);
        return dailyCount;
    }

    private int saveMarginDailyList(List<MarginDaily> dailyList, String exchange, LocalDate tradeDate) {
        // Query existing for this exchange + trade date combo
        List<MarginDaily> existing = marginDailyMapper.selectList(
                new LambdaQueryWrapper<MarginDaily>()
                        .eq(MarginDaily::getExchange, exchange)
                        .eq(MarginDaily::getTradeDate, tradeDate)
        );

        Map<String, MarginDaily> existingByCode = existing.stream()
                .collect(Collectors.toMap(MarginDaily::getStockCode, e -> e, (a, b) -> a));

        int count = 0;
        for (MarginDaily entity : dailyList) {
            MarginDaily existingEntity = existingByCode.get(entity.getStockCode());
            if (existingEntity != null) {
                entity.setId(existingEntity.getId());
                marginDailyMapper.updateById(entity);
            } else {
                marginDailyMapper.insert(entity);
            }
            count++;
        }
        return count;
    }

    private void saveMarginStocks(List<MarginDaily> dailyList, String exchange, LocalDate tradeDate) {
        // Query existing margin stocks for this exchange
        List<MarginStock> existing = marginStockMapper.selectList(
                new LambdaQueryWrapper<MarginStock>()
                        .eq(MarginStock::getExchange, exchange)
                        .eq(MarginStock::getSnapDate, tradeDate)
        );

        Map<String, MarginStock> existingByCode = existing.stream()
                .collect(Collectors.toMap(MarginStock::getStockCode, e -> e, (a, b) -> a));

        // Deduplicate by stock code (name from the first occurrence)
        Map<String, String> stockNameMap = dailyList.stream()
                .filter(d -> d.getStockCode() != null)
                .collect(Collectors.toMap(
                        MarginDaily::getStockCode,
                        d -> d.getStockCode(), // use code as name fallback
                        (a, b) -> a
                ));

        int saved = 0;
        for (Map.Entry<String, String> entry : stockNameMap.entrySet()) {
            String stockCode = entry.getKey();
            MarginStock existingStock = existingByCode.get(stockCode);
            if (existingStock != null) {
                existingStock.setIsMargin(1);
                existingStock.setIsShort(1);
                existingStock.setSnapDate(tradeDate);
                marginStockMapper.updateById(existingStock);
            } else {
                MarginStock marginStock = new MarginStock();
                marginStock.setStockCode(stockCode);
                marginStock.setStockName(stockCode); // stock name not available in margin detail API, use code
                marginStock.setExchange(exchange);
                marginStock.setIsMargin(1);
                marginStock.setIsShort(1);
                marginStock.setSnapDate(tradeDate);
                marginStockMapper.insert(marginStock);
            }
            saved++;
        }
        log.info("Saved {} margin_stock records for {} on {}", saved, exchange, tradeDate);
    }

    private List<MarginDaily> parseMarginDailyList(String rawJson, String exchange, LocalDate tradeDate) {
        List<MarginDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for margin detail, got: {}", root.getNodeType());
                return result;
            }

            for (JsonNode node : root) {
                MarginDaily daily = parseMarginDaily(node, exchange, tradeDate);
                if (daily != null && daily.getStockCode() != null && !daily.getStockCode().isEmpty()) {
                    result.add(daily);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse margin detail JSON for {} on {}", exchange, tradeDate, e);
            throw new RuntimeException("Failed to parse margin detail data: " + e.getMessage(), e);
        }
        return result;
    }

    private MarginDaily parseMarginDaily(JsonNode node, String exchange, LocalDate tradeDate) {
        MarginDaily daily = new MarginDaily();
        daily.setStockCode(safeText(node, "股票代码"));
        daily.setTradeDate(tradeDate);
        daily.setExchange(exchange);
        daily.setMarginBalance(safeDecimal(node, "融资余额"));
        daily.setMarginBuy(safeDecimal(node, "融资买入额"));
        daily.setMarginRepay(safeDecimal(node, "融资偿还额"));
        daily.setShortBalance(safeDecimal(node, "融券余额"));
        daily.setShortSellVol(safeLong(node, "融券卖出量"));
        daily.setShortRepayVol(safeLong(node, "融券偿还量"));
        daily.setShortRemainVol(safeLong(node, "融券余量"));
        daily.setTotalBalance(safeDecimal(node, "总余额"));
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
