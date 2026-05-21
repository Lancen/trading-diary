package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.entity.MarginStock;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginStockMapper;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarginCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginCleanseService.class);

    private final MarginDailyMapper marginDailyMapper;
    private final MarginStockMapper marginStockMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public MarginCleanseService(MarginDailyMapper marginDailyMapper, MarginStockMapper marginStockMapper,
                                 BatchSqlRunner batchSqlRunner, ObjectMapper objectMapper) {
        this.marginDailyMapper = marginDailyMapper;
        this.marginStockMapper = marginStockMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    /**
     * 清洗两融明细数据
     * <p>
     * 从原始JSON数据中解析两融明细信息，包括融资余额、融券余量等。
     * 同时维护两融标的股票列表，确保新标的股票被记录。
     * </p>
     *
     * @param rawJson 原始JSON数据字符串
     * @param exchange 交易所代码，"SSE"表示沪市，"SZSE"表示深市
     * @param tradeDate 交易日期
     * @return 处理的两融明细记录数量
     */
    @Transactional
    public int cleanse(String rawJson, String exchange, LocalDate tradeDate) {
        List<MarginDaily> dailyList = parseMarginDailyList(rawJson, exchange, tradeDate);
        if (dailyList.isEmpty()) {
            log.warn("No margin daily records parsed for {} on {}", exchange, tradeDate);
            return 0;
        }

        // 计算环比变化：查询上一交易日数据，差额写入 marginChange/shortChange
        LocalDate prevTradeDate = findPreviousTradeDate(tradeDate, exchange);
        Map<String, MarginDaily> prevByCode = Collections.emptyMap();
        if (prevTradeDate != null) {
            prevByCode = marginDailyMapper.selectList(
                    new LambdaQueryWrapper<MarginDaily>()
                            .eq(MarginDaily::getExchange, exchange)
                            .eq(MarginDaily::getTradeDate, prevTradeDate)
            ).stream().collect(Collectors.toMap(MarginDaily::getStockCode, e -> e, (a, b) -> a));
        }
        final Map<String, MarginDaily> prevMap = prevByCode;
        for (MarginDaily entity : dailyList) {
            MarginDaily prev = prevMap.get(entity.getStockCode());
            if (prev != null && entity.getMarginBalance() != null && prev.getMarginBalance() != null) {
                entity.setMarginChange(entity.getMarginBalance().subtract(prev.getMarginBalance()));
            }
            if (prev != null && entity.getShortBalance() != null && prev.getShortBalance() != null) {
                entity.setShortChange(entity.getShortBalance().subtract(prev.getShortBalance()));
            }
        }

        int dailyCount = saveMarginDailyBatch(dailyList, exchange, tradeDate);
        saveMarginStocksBatch(dailyList, exchange, tradeDate);

        log.info("Margin cleanse complete: {} margin_daily records for {} on {}",
                dailyCount, exchange, tradeDate);
        return dailyCount;
    }

    private int saveMarginDailyBatch(List<MarginDaily> dailyList, String exchange, LocalDate tradeDate) {
        List<MarginDaily> existing = marginDailyMapper.selectList(
                new LambdaQueryWrapper<MarginDaily>()
                        .eq(MarginDaily::getExchange, exchange)
                        .eq(MarginDaily::getTradeDate, tradeDate)
        );

        Map<String, MarginDaily> existingByCode = existing.stream()
                .collect(Collectors.toMap(MarginDaily::getStockCode, e -> e, (a, b) -> a));

        List<MarginDaily> toInsert = new ArrayList<>();
        List<MarginDaily> toUpdate = new ArrayList<>();

        for (MarginDaily entity : dailyList) {
            MarginDaily existingEntity = existingByCode.get(entity.getStockCode());
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
        return count;
    }

    private void saveMarginStocksBatch(List<MarginDaily> dailyList, String exchange, LocalDate tradeDate) {
        Set<String> stockCodes = new HashSet<>();
        for (MarginDaily d : dailyList) {
            if (d.getStockCode() != null) stockCodes.add(d.getStockCode());
        }

        List<MarginStock> existing = marginStockMapper.selectList(
                new LambdaQueryWrapper<MarginStock>()
                        .eq(MarginStock::getExchange, exchange)
                        .eq(MarginStock::getSnapDate, tradeDate)
        );

        Map<String, MarginStock> existingByCode = existing.stream()
                .collect(Collectors.toMap(MarginStock::getStockCode, e -> e, (a, b) -> a));

        List<MarginStock> toInsert = new ArrayList<>();
        for (String code : stockCodes) {
            if (!existingByCode.containsKey(code)) {
                MarginStock ms = new MarginStock();
                ms.setStockCode(code);
                ms.setStockName(code);
                ms.setExchange(exchange);
                ms.setSnapDate(tradeDate);
                toInsert.add(ms);
            }
        }

        if (!toInsert.isEmpty()) {
            batchSqlRunner.batchInsert(toInsert);
        }
    }

    private List<MarginDaily> parseMarginDailyList(String rawJson, String exchange, LocalDate tradeDate) {
        List<MarginDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for margin detail, got: {}", root.getNodeType());
                return result;
            }

            boolean isSZSE = "SZSE".equals(exchange);
            for (JsonNode node : root) {
                MarginDaily daily = new MarginDaily();
                // SSE: 标的证券代码/简称, SZSE: 证券代码/简称
                String code = isSZSE ? safeText(node, "证券代码") : safeText(node, "标的证券代码");
                daily.setStockCode(code);
                daily.setTradeDate(tradeDate);
                daily.setExchange(exchange);
                daily.setMarginBalance(safeDecimal(node, "融资余额"));
                daily.setMarginBuy(safeDecimal(node, "融资买入额"));
                daily.setMarginRepay(safeDecimal(node, "融资偿还额")); // SZSE 无此字段
                daily.setShortBalance(safeDecimal(node, "融券余额"));
                daily.setShortSellVol(safeLong(node, "融券卖出量"));
                daily.setShortRepayVol(safeLong(node, "融券偿还量")); // SZSE 无此字段
                daily.setShortRemainVol(safeLong(node, "融券余量"));
                // SZSE: 融资融券余额(汇总), SSE: 无此字段(需计算)
                daily.setTotalBalance(safeDecimal(node, "融资融券余额"));

                if (daily.getStockCode() != null && !daily.getStockCode().isEmpty()) {
                    result.add(daily);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse margin detail JSON for {} on {}", exchange, tradeDate, e);
            throw new RuntimeException("解析两融明细数据失败: " + e.getMessage(), e);
        }
        return result;
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

    private LocalDate findPreviousTradeDate(LocalDate tradeDate, String exchange) {
        return marginDailyMapper.selectList(
                new LambdaQueryWrapper<MarginDaily>()
                        .select(MarginDaily::getTradeDate)
                        .eq(MarginDaily::getExchange, exchange)
                        .lt(MarginDaily::getTradeDate, tradeDate)
                        .orderByDesc(MarginDaily::getTradeDate)
                        .last("LIMIT 1")
        ).stream().findFirst().map(MarginDaily::getTradeDate).orElse(null);
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