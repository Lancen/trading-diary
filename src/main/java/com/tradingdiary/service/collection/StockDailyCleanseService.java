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

    /**
     * 清洗股票日线数据
     * <p>
     * 从原始JSON数据中解析股票日线信息，并与数据库中已有数据进行对比。
     * 新数据执行插入操作，已存在数据执行更新操作。
     * </p>
     *
     * @param rawJson 原始JSON数据字符串
     * @param tradeDate 交易日期
     * @return 处理的记录总数（插入+更新）
     */
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

    /**
     * 批量清洗历史股票日线数据
     * <p>
     * 处理多只股票的历史日线数据，支持批量导入场景。
     * 自动识别新增和更新记录，执行相应的数据库操作。
     * </p>
     *
     * @param rawJsonList 原始JSON数据列表，每个元素对应一只股票的数据
     * @param stockCodes 股票代码列表，与rawJsonList一一对应
     * @return 处理的记录总数（插入+更新）
     */
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

    /**
     * 清洗单只股票的历史日线数据
     * <p>
     * 处理单只股票的历史日线数据，自动识别新增和更新记录。
     * 适用于单只股票的数据补采场景。
     * </p>
     *
     * @param rawJson 原始JSON数据字符串
     * @param stockCode 股票代码
     * @return 处理的记录总数（插入+更新）
     */
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

    /**
     * 清洗 Tushare daily API 返回的全市场日线数据
     * <p>
     * Tushare 返回格式: {"fields": [...], "items": [[...], ...]}，items 是二维数组。
     * </p>
     *
     * @param tushareResponse Tushare API 原始返回 JSON
     * @return 处理的记录总数（插入+更新）
     */
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

    List<StockDaily> parseTushareDaily(String tushareResponse) {
        List<StockDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(tushareResponse);
            JsonNode data = root.path("data");
            JsonNode fields = data.path("fields");
            JsonNode items = data.path("items");
            if (!items.isArray()) return result;

            // 找到各字段的索引位置
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
                // ts_code: "000001.SZ" → "000001"
                String tsCode = item.get(idxCode).asText();
                daily.setStockCode(tsCode.contains(".") ? tsCode.substring(0, tsCode.indexOf('.')) : tsCode);
                // trade_date: "20260521" → LocalDate
                daily.setTradeDate(LocalDate.parse(
                        item.get(idxDate).asText().replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3")));
                daily.setOpen(safeBigDecimal(item, idxOpen));
                daily.setHigh(safeBigDecimal(item, idxHigh));
                daily.setLow(safeBigDecimal(item, idxLow));
                daily.setClose(safeBigDecimal(item, idxClose));
                // vol: 手 → 股 (×100)
                Long vol = safeLong(item, idxVol);
                daily.setVolume(vol != null ? vol * 100 : null);
                // amount: 千元 → 元 (×1000)
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