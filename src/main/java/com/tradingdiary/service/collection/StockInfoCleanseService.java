package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.mapper.StockInfoMapper;
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
public class StockInfoCleanseService {

    private static final Logger log = LoggerFactory.getLogger(StockInfoCleanseService.class);

    private final StockInfoMapper stockInfoMapper;
    private final ObjectMapper objectMapper;

    public StockInfoCleanseService(StockInfoMapper stockInfoMapper, ObjectMapper objectMapper) {
        this.stockInfoMapper = stockInfoMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Cleanse stock_zh_a_spot_em JSON into StockInfo entities.
     * Uses INSERT ON DUPLICATE KEY UPDATE pattern: query existing by unique key,
     * set ID on matched entities, then saveOrUpdate.
     *
     * @param rawJson      raw JSON from stock_zh_a_spot_em API
     * @param snapshotDate the snapshot date
     * @return number of records inserted/updated
     */
    public int cleanse(String rawJson, LocalDate snapshotDate) {
        List<StockInfo> entities = parseStockInfoList(rawJson, snapshotDate);
        if (entities.isEmpty()) {
            log.warn("No stock info records parsed for {}", snapshotDate);
            return 0;
        }

        // Query existing records for this snapshot date
        List<StockInfo> existing = stockInfoMapper.selectList(
                new LambdaQueryWrapper<StockInfo>()
                        .eq(StockInfo::getSnapshotDate, snapshotDate)
        );

        Map<String, StockInfo> existingByCode = existing.stream()
                .collect(Collectors.toMap(StockInfo::getCode, e -> e, (a, b) -> a));

        int count = 0;
        for (StockInfo entity : entities) {
            StockInfo existingEntity = existingByCode.get(entity.getCode());
            if (existingEntity != null) {
                entity.setId(existingEntity.getId());
                stockInfoMapper.updateById(entity);
            } else {
                try {
                    stockInfoMapper.insert(entity);
                } catch (DuplicateKeyException e) {
                    // Race condition: another thread inserted, query and update
                    StockInfo race = stockInfoMapper.selectOne(
                            new LambdaQueryWrapper<StockInfo>()
                                    .eq(StockInfo::getCode, entity.getCode())
                                    .eq(StockInfo::getSnapshotDate, snapshotDate)
                    );
                    if (race != null) {
                        entity.setId(race.getId());
                        stockInfoMapper.updateById(entity);
                    } else {
                        throw e;
                    }
                }
            }
            count++;
        }

        log.info("StockInfo cleanse complete: {} records for {}", count, snapshotDate);
        return count;
    }

    private List<StockInfo> parseStockInfoList(String rawJson, LocalDate snapshotDate) {
        List<StockInfo> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for stock info, got: {}", root.getNodeType());
                return result;
            }

            for (JsonNode node : root) {
                StockInfo info = parseStockInfo(node, snapshotDate);
                if (info != null && info.getCode() != null && !info.getCode().isEmpty()) {
                    result.add(info);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse stock info JSON", e);
            throw new RuntimeException("Failed to parse stock info data: " + e.getMessage(), e);
        }
        return result;
    }

    private StockInfo parseStockInfo(JsonNode node, LocalDate snapshotDate) {
        StockInfo info = new StockInfo();
        info.setCode(safeText(node, "代码"));
        info.setName(safeText(node, "名称"));
        info.setLatestPrice(safeDecimal(node, "最新价"));
        info.setChangePct(safeDecimal(node, "涨跌幅"));
        info.setChangeAmount(safeDecimal(node, "涨跌额"));
        info.setVolume(safeLong(node, "成交量"));
        info.setAmount(safeDecimal(node, "成交额"));
        info.setTurnoverRate(safeDecimal(node, "换手率"));
        info.setVolumeRatio(safeDecimal(node, "量比"));
        info.setPe(safeDecimal(node, "市盈率-动态"));
        info.setPb(safeDecimal(node, "市净率"));
        info.setTotalMv(safeDecimal(node, "总市值"));
        info.setFloatMv(safeDecimal(node, "流通市值"));
        info.setSnapshotDate(snapshotDate);
        return info;
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
