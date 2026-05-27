package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.service.collection.StockInfoCleanseService;
import com.tradingdiary.util.BatchSqlRunner;
import com.tradingdiary.util.JsonNodeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.tradingdiary.util.JsonNodeHelper.*;
import static com.tradingdiary.util.UpsertHelper.partitionAndSave;

/**
 * 股票信息清洗服务实现，解析股票行情快照 JSON 并入库
 */
@Service
public class StockInfoCleanseServiceImpl implements StockInfoCleanseService {

    private static final Logger log = LoggerFactory.getLogger(StockInfoCleanseServiceImpl.class);
    private final StockInfoMapper stockInfoMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public StockInfoCleanseServiceImpl(StockInfoMapper stockInfoMapper, BatchSqlRunner batchSqlRunner,
                                        ObjectMapper objectMapper) {
        this.stockInfoMapper = stockInfoMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
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

        int count = partitionAndSave(entities, existing,
                StockInfo::getCode, (src, tgt) -> tgt.setId(src.getId()), batchSqlRunner);

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
            throw new RuntimeException("解析股票基础信息数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private StockInfo parseStockInfo(JsonNode node, LocalDate snapshotDate) {
        StockInfo info = new StockInfo();
        String code = safeText(node, "代码");
        info.setCode(code);
        if (code != null && code.length() > 2) {
            String prefix = code.substring(0, 2).toLowerCase();
            if ("sh".equals(prefix) || "sz".equals(prefix) || "bj".equals(prefix)) {
                info.setMarket(prefix.toUpperCase());
                info.setStockCode(code.substring(2));
            } else {
                info.setMarket("OTHER");
                info.setStockCode(code);
            }
        } else if (code != null) {
            info.setMarket("OTHER");
            info.setStockCode(code);
        }
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

}
