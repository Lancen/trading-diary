package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.mapper.StockDailyMapper;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockDailyCleanseService {

    private static final Logger log = LoggerFactory.getLogger(StockDailyCleanseService.class);
    private static final int BATCH_SIZE = 500;

    private final StockDailyMapper stockDailyMapper;
    private final ObjectMapper objectMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public StockDailyCleanseService(StockDailyMapper stockDailyMapper, ObjectMapper objectMapper,
                                     SqlSessionFactory sqlSessionFactory) {
        this.stockDailyMapper = stockDailyMapper;
        this.objectMapper = objectMapper;
        this.sqlSessionFactory = sqlSessionFactory;
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
            count += executeBatch(toInsert, (mapper, e) -> mapper.insert(e));
        }
        if (!toUpdate.isEmpty()) {
            count += executeBatch(toUpdate, (mapper, e) -> mapper.updateById(e));
        }

        log.info("StockDaily cleanse complete: {} records (insert={}, update={})",
                count, toInsert.size(), toUpdate.size());
        return count;
    }

    /**
     * 批量清洗多只股票的历史日线数据，所有写入共用同一个 BATCH SqlSession。
     */
    @Transactional
    public int cleanseHistBatch(List<String> rawJsonList, List<String> stockCodes) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            StockDailyMapper mapper = session.getMapper(StockDailyMapper.class);
            int total = 0;
            for (int i = 0; i < rawJsonList.size(); i++) {
                List<StockDaily> entities = parseHistStockDailyListTx(rawJsonList.get(i), stockCodes.get(i));
                for (StockDaily e : entities) {
                    List<StockDaily> existing = mapper.selectList(
                            new LambdaQueryWrapper<StockDaily>()
                                    .eq(StockDaily::getStockCode, e.getStockCode())
                                    .eq(StockDaily::getTradeDate, e.getTradeDate())
                    );
                    if (!existing.isEmpty()) {
                        e.setId(existing.get(0).getId());
                        mapper.updateById(e);
                    } else {
                        mapper.insert(e);
                    }
                    total++;
                }
                if (total > 0 && total % 500 == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
            return total;
        }
    }

    @Transactional
    public int cleanseHistJson(String rawJson, String stockCode) {
        List<StockDaily> entities = parseHistStockDailyListTx(rawJson, stockCode);
        if (entities.isEmpty()) {
            log.debug("No hist records parsed for {}", stockCode);
            return 0;
        }

        return executeBatch(entities, (mapper, e) -> {
            List<StockDaily> existing = mapper.selectList(
                    new LambdaQueryWrapper<StockDaily>()
                            .eq(StockDaily::getStockCode, e.getStockCode())
                            .eq(StockDaily::getTradeDate, e.getTradeDate())
            );
            if (!existing.isEmpty()) {
                e.setId(existing.get(0).getId());
                mapper.updateById(e);
            } else {
                mapper.insert(e);
            }
        });
    }

    private int executeBatch(List<StockDaily> list,
                              java.util.function.BiConsumer<StockDailyMapper, StockDaily> operation) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            StockDailyMapper mapper = session.getMapper(StockDailyMapper.class);
            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                operation.accept(mapper, list.get(i));
                count++;
                if ((i + 1) % BATCH_SIZE == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
            return count;
        }
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
