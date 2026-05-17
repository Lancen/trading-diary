package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.entity.MarginStock;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginStockMapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarginCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginCleanseService.class);
    private static final int BATCH_SIZE = 500;

    private final MarginDailyMapper marginDailyMapper;
    private final MarginStockMapper marginStockMapper;
    private final ObjectMapper objectMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public MarginCleanseService(MarginDailyMapper marginDailyMapper, MarginStockMapper marginStockMapper,
                                 ObjectMapper objectMapper, SqlSessionFactory sqlSessionFactory) {
        this.marginDailyMapper = marginDailyMapper;
        this.marginStockMapper = marginStockMapper;
        this.objectMapper = objectMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Transactional
    public int cleanse(String rawJson, String exchange, LocalDate tradeDate) {
        List<MarginDaily> dailyList = parseMarginDailyList(rawJson, exchange, tradeDate);
        if (dailyList.isEmpty()) {
            log.warn("No margin daily records parsed for {} on {}", exchange, tradeDate);
            return 0;
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
            count += executeDailyBatch(toInsert, BATCH_SIZE, (mapper, e) -> mapper.insert(e));
        }
        if (!toUpdate.isEmpty()) {
            count += executeDailyBatch(toUpdate, BATCH_SIZE, (mapper, e) -> mapper.updateById(e));
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
                ms.setExchange(exchange);
                ms.setSnapDate(tradeDate);
                toInsert.add(ms);
            }
        }

        if (!toInsert.isEmpty()) {
            executeStockBatch(toInsert, BATCH_SIZE, (mapper, e) -> mapper.insert(e));
        }
    }

    private int executeDailyBatch(List<MarginDaily> list, int batchSize,
                                   java.util.function.BiConsumer<MarginDailyMapper, MarginDaily> op) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            MarginDailyMapper mapper = session.getMapper(MarginDailyMapper.class);
            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                op.accept(mapper, list.get(i));
                count++;
                if ((i + 1) % batchSize == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
            return count;
        }
    }

    private void executeStockBatch(List<MarginStock> list, int batchSize,
                                    java.util.function.BiConsumer<MarginStockMapper, MarginStock> op) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            MarginStockMapper mapper = session.getMapper(MarginStockMapper.class);
            for (int i = 0; i < list.size(); i++) {
                op.accept(mapper, list.get(i));
                if ((i + 1) % batchSize == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
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

            for (JsonNode node : root) {
                MarginDaily daily = new MarginDaily();
                daily.setStockCode(safeText(node, "标的证券代码"));
                daily.setTradeDate(tradeDate);
                daily.setExchange(exchange);
                daily.setMarginBalance(safeDecimal(node, "融资余额"));
                daily.setMarginBuy(safeDecimal(node, "融资买入额"));
                daily.setMarginRepay(safeDecimal(node, "融资偿还额"));
                daily.setShortBalance(safeDecimal(node, "融券余额"));
                daily.setShortSellVol(safeLong(node, "融券卖出量"));
                daily.setShortRepayVol(safeLong(node, "融券偿还量"));
                daily.setShortRemainVol(safeLong(node, "融券余量"));
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
