package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.service.collection.StockDailyCleanseService;
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

/**
 * 股票日线清洗服务实现，解析股票日行情 JSON 并入库
 */
@Service
public class StockDailyCleanseServiceImpl implements StockDailyCleanseService {

    private static final Logger log = LoggerFactory.getLogger(StockDailyCleanseServiceImpl.class);
    private final StockDailyMapper stockDailyMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public StockDailyCleanseServiceImpl(StockDailyMapper stockDailyMapper, BatchSqlRunner batchSqlRunner,
                                         ObjectMapper objectMapper) {
        this.stockDailyMapper = stockDailyMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
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
        String rawCode = safeText(node, "代码");
        daily.setStockCode(stripMarketPrefix(rawCode));
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
            log.debug("解析BigDecimal失败: field={}", field, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public int cleanseTushareDaily(String tushareResponse) {
        List<StockDaily> entities = parseTushareDaily(tushareResponse);
        if (entities.isEmpty()) {
            log.warn("No records parsed from Tushare daily response");
            return 0;
        }

        LocalDate tradeDate = entities.get(0).getTradeDate();
        List<StockDaily> existing = stockDailyMapper.selectList(
                new LambdaQueryWrapper<StockDaily>().eq(StockDaily::getTradeDate, tradeDate));

        Map<String, StockDaily> existingByCode = existing.stream()
                .collect(Collectors.toMap(StockDaily::getStockCode, e -> e, (a, b) -> a));

        List<StockDaily> toInsert = new ArrayList<>();
        List<StockDaily> toUpdate = new ArrayList<>();
        for (StockDaily entity : entities) {
            StockDaily exist = existingByCode.get(entity.getStockCode());
            if (exist != null) {
                entity.setId(exist.getId());
                toUpdate.add(entity);
            } else {
                toInsert.add(entity);
            }
        }

        int count = 0;
        if (!toInsert.isEmpty()) count += batchSqlRunner.batchInsert(toInsert);
        if (!toUpdate.isEmpty()) count += batchSqlRunner.batchUpdate(toUpdate);

        log.info("Tushare daily cleanse: {} records (insert={}, update={}) for {}",
                count, toInsert.size(), toUpdate.size(), tradeDate);
        return count;
    }

    @Override
    public List<StockDaily> parseTushareDaily(String tushareResponse) {
        List<StockDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(tushareResponse);
            JsonNode data = root.path("data");
            JsonNode fields = data.path("fields");
            JsonNode items = data.path("items");
            if (!items.isArray()) return result;

            int idxCode = -1, idxDate = -1, idxOpen = -1, idxHigh = -1, idxLow = -1;
            int idxClose = -1, idxVol = -1, idxAmount = -1;
            for (int i = 0; i < fields.size(); i++) {
                String f = fields.get(i).asText();
                switch (f) {
                    case "ts_code": idxCode = i; break;
                    case "trade_date": idxDate = i; break;
                    case "open": idxOpen = i; break;
                    case "high": idxHigh = i; break;
                    case "low": idxLow = i; break;
                    case "close": idxClose = i; break;
                    case "vol": idxVol = i; break;
                    case "amount": idxAmount = i; break;
                }
            }

            for (JsonNode item : items) {
                StockDaily daily = new StockDaily();
                String tsCode = item.get(idxCode).asText();
                daily.setStockCode(tsCode.contains(".") ? tsCode.substring(0, tsCode.indexOf('.')) : tsCode);
                daily.setTradeDate(LocalDate.parse(
                        item.get(idxDate).asText().replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3")));
                daily.setOpen(safeBigDecimal(item, idxOpen));
                daily.setHigh(safeBigDecimal(item, idxHigh));
                daily.setLow(safeBigDecimal(item, idxLow));
                daily.setClose(safeBigDecimal(item, idxClose));
                Long vol = safeLong(item, idxVol);
                daily.setVolume(vol != null ? vol * 100 : null);
                BigDecimal amt = safeBigDecimal(item, idxAmount);
                daily.setAmount(amt != null ? amt.multiply(java.math.BigDecimal.valueOf(1000)) : null);
                result.add(daily);
            }
        } catch (Exception e) {
            log.error("Failed to parse Tushare daily response", e);
            throw new RuntimeException("解析 Tushare 日线数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private BigDecimal safeBigDecimal(JsonNode node, int index) {
        if (index < 0 || index >= node.size()) return null;
        JsonNode val = node.get(index);
        if (val == null || val.isNull()) return null;
        try {
            return new BigDecimal(val.asText());
        } catch (Exception e) {
            log.debug("解析BigDecimal失败: index={}", index, e.getMessage());
            return null;
        }
    }

    private Long safeLong(JsonNode node, int index) {
        if (index < 0 || index >= node.size()) return null;
        JsonNode val = node.get(index);
        if (val == null || val.isNull()) return null;
        try {
            return Long.parseLong(val.asText());
        } catch (Exception e) {
            log.debug("解析Long失败: index={}", index, e.getMessage());
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

    /**
     * 剥离股票代码的市场前缀（sh/sz/bj）
     *
     * @param code 原始代码，如 "sh600000" 或 "600000"
     * @return 剥离前缀后的代码，如 "600000"；无效输入返回原值
     */
    private String stripMarketPrefix(String code) {
        if (code == null || code.isEmpty()) return code;
        if (code.length() > 2) {
            String prefix = code.substring(0, 2).toLowerCase();
            if ("sh".equals(prefix) || "sz".equals(prefix) || "bj".equals(prefix)) {
                return code.substring(2);
            }
        }
        return code;
    }
}
