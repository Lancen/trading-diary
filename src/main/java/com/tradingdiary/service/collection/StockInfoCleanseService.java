package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.CollectionConstants;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.mapper.StockInfoMapper;
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
public class StockInfoCleanseService {

    private static final Logger log = LoggerFactory.getLogger(StockInfoCleanseService.class);
    private final StockInfoMapper stockInfoMapper;
    private final ObjectMapper objectMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public StockInfoCleanseService(StockInfoMapper stockInfoMapper, ObjectMapper objectMapper,
                                    SqlSessionFactory sqlSessionFactory) {
        this.stockInfoMapper = stockInfoMapper;
        this.objectMapper = objectMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Transactional
    public int cleanse(String rawJson, LocalDate snapshotDate) {
        List<StockInfo> entities = parseStockInfoList(rawJson, snapshotDate);
        if (entities.isEmpty()) {
            log.warn("No stock info records parsed for {}", snapshotDate);
            return 0;
        }

        List<StockInfo> existing = stockInfoMapper.selectList(
                new LambdaQueryWrapper<StockInfo>()
                        .eq(StockInfo::getSnapshotDate, snapshotDate)
        );

        Map<String, StockInfo> existingByCode = existing.stream()
                .collect(Collectors.toMap(StockInfo::getCode, e -> e, (a, b) -> a));

        List<StockInfo> toInsert = new ArrayList<>();
        List<StockInfo> toUpdate = new ArrayList<>();

        for (StockInfo entity : entities) {
            StockInfo existingEntity = existingByCode.get(entity.getCode());
            if (existingEntity != null) {
                entity.setId(existingEntity.getId());
                toUpdate.add(entity);
            } else {
                toInsert.add(entity);
            }
        }

        int count = 0;
        if (!toInsert.isEmpty()) {
            count += executeBatch(toInsert, CollectionConstants.DB_BATCH_SIZE, (mapper, e) -> mapper.insert(e));
        }
        if (!toUpdate.isEmpty()) {
            count += executeBatch(toUpdate, CollectionConstants.DB_BATCH_SIZE, (mapper, e) -> mapper.updateById(e));
        }

        log.info("StockInfo cleanse complete: {} records (insert={}, update={})",
                count, toInsert.size(), toUpdate.size());
        return count;
    }

    /**
     * 使用 MyBatis BATCH 模式执行批处理，配合 rewriteBatchedStatements=true 实现真正的 JDBC 批量写入。
     */
    private int executeBatch(List<StockInfo> list, int batchSize,
                              java.util.function.BiConsumer<StockInfoMapper, StockInfo> operation) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            StockInfoMapper mapper = session.getMapper(StockInfoMapper.class);
            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                operation.accept(mapper, list.get(i));
                count++;
                if ((i + 1) % batchSize == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
            return count;
        }
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
            throw new RuntimeException("解析股票基础信息数据失败: " + e.getMessage(), e);
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
