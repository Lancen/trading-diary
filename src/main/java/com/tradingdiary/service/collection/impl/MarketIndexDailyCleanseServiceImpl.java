package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.mapper.MarketIndexDailyMapper;
import com.tradingdiary.service.collection.MarketIndexDailyCleanseService;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarketIndexDailyCleanseServiceImpl implements MarketIndexDailyCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarketIndexDailyCleanseServiceImpl.class);

    private final MarketIndexDailyMapper marketIndexDailyMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public MarketIndexDailyCleanseServiceImpl(MarketIndexDailyMapper marketIndexDailyMapper,
                                               BatchSqlRunner batchSqlRunner,
                                               ObjectMapper objectMapper) {
        this.marketIndexDailyMapper = marketIndexDailyMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int cleanse(String rawJson, String indexCode) {
        List<MarketIndexDaily> entities = parseMarketIndexDailyList(rawJson, indexCode);
        if (entities.isEmpty()) {
            log.warn("No market index daily records parsed for {}", indexCode);
            return 0;
        }

        Map<LocalDate, MarketIndexDaily> prevMap = findPreviousCloseMap(indexCode, entities);

        for (MarketIndexDaily entity : entities) {
            MarketIndexDaily prev = prevMap.get(entity.getTradeDate().minusDays(1));
            if (prev != null && prev.getClose() != null && entity.getClose() != null
                    && prev.getClose().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal changePct = entity.getClose().subtract(prev.getClose())
                        .divide(prev.getClose(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                entity.setChangePct(changePct);
            }
        }

        return saveBatch(entities, indexCode);
    }

    private List<MarketIndexDaily> parseMarketIndexDailyList(String rawJson, String indexCode) {
        List<MarketIndexDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for market index daily, got: {}", root.getNodeType());
                return result;
            }

            for (JsonNode node : root) {
                MarketIndexDaily daily = new MarketIndexDaily();
                daily.setIndexCode(indexCode);
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
            log.error("Failed to parse market index daily JSON for {}", indexCode, e);
            throw new RuntimeException("解析宽基指数日线数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private Map<LocalDate, MarketIndexDaily> findPreviousCloseMap(String indexCode, List<MarketIndexDaily> entities) {
        if (entities.isEmpty()) return Collections.emptyMap();

        LocalDate minDate = entities.stream().map(MarketIndexDaily::getTradeDate).min(LocalDate::compareTo).orElse(null);
        if (minDate == null) return Collections.emptyMap();

        List<MarketIndexDaily> existing = marketIndexDailyMapper.selectList(
                new LambdaQueryWrapper<MarketIndexDaily>()
                        .eq(MarketIndexDaily::getIndexCode, indexCode)
                        .lt(MarketIndexDaily::getTradeDate, minDate)
                        .orderByDesc(MarketIndexDaily::getTradeDate)
        );

        Map<LocalDate, MarketIndexDaily> allByDate = existing.stream()
                .collect(Collectors.toMap(MarketIndexDaily::getTradeDate, e -> e, (a, b) -> a));

        for (MarketIndexDaily e : entities) {
            allByDate.put(e.getTradeDate(), e);
        }

        return allByDate;
    }

    private int saveBatch(List<MarketIndexDaily> entities, String indexCode) {
        List<MarketIndexDaily> existing = marketIndexDailyMapper.selectList(
                new LambdaQueryWrapper<MarketIndexDaily>()
                        .eq(MarketIndexDaily::getIndexCode, indexCode)
        );

        Map<LocalDate, MarketIndexDaily> existingByDate = existing.stream()
                .collect(Collectors.toMap(MarketIndexDaily::getTradeDate, e -> e, (a, b) -> a));

        List<MarketIndexDaily> toInsert = new ArrayList<>();
        List<MarketIndexDaily> toUpdate = new ArrayList<>();

        for (MarketIndexDaily entity : entities) {
            MarketIndexDaily existingEntity = existingByDate.get(entity.getTradeDate());
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

        log.info("MarketIndexDaily cleanse complete: {} records for {} (insert={}, update={})",
                count, indexCode, toInsert.size(), toUpdate.size());
        return count;
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
            log.debug("解析BigDecimal失败: field={}", field, e.getMessage());
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
            log.debug("解析Long失败: field={}", field, e.getMessage());
            return null;
        }
    }
}
