package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.mapper.SectorIndexDailyMapper;
import com.tradingdiary.service.collection.SectorIndexDailyCleanseService;
import com.tradingdiary.util.BatchSqlRunner;
import com.tradingdiary.util.JsonNodeHelper;
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

import static com.tradingdiary.util.JsonNodeHelper.*;
import static com.tradingdiary.util.UpsertHelper.partitionAndSave;

/**
 * 板块指数日线清洗服务实现，解析板块 OHLCV JSON 并入库
 */
@Service
public class SectorIndexDailyCleanseServiceImpl implements SectorIndexDailyCleanseService {

    private static final Logger log = LoggerFactory.getLogger(SectorIndexDailyCleanseServiceImpl.class);

    private final SectorIndexDailyMapper sectorIndexDailyMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public SectorIndexDailyCleanseServiceImpl(SectorIndexDailyMapper sectorIndexDailyMapper,
                                               BatchSqlRunner batchSqlRunner,
                                               ObjectMapper objectMapper) {
        this.sectorIndexDailyMapper = sectorIndexDailyMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int cleanse(String rawJson, String sectorType, String sectorCode) {
        List<SectorIndexDaily> entities = parseSectorIndexDailyList(rawJson, sectorType, sectorCode);
        if (entities.isEmpty()) {
            log.warn("No sector index daily records parsed for {}/{}", sectorType, sectorCode);
            return 0;
        }

        computeChangePct(entities, sectorType, sectorCode);

        return saveBatch(entities, sectorType, sectorCode);
    }

    private List<SectorIndexDaily> parseSectorIndexDailyList(String rawJson, String sectorType, String sectorCode) {
        List<SectorIndexDaily> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                log.warn("Expected JSON array for sector index daily, got: {}", root.getNodeType());
                return result;
            }

            for (JsonNode node : root) {
                SectorIndexDaily daily = new SectorIndexDaily();
                daily.setSectorType(sectorType);
                daily.setSectorCode(sectorCode);
                String dateStr = safeText(node, "日期");
                if (dateStr == null || dateStr.isEmpty()) continue;
                try {
                    daily.setTradeDate(LocalDate.parse(dateStr.substring(0, 10)));
                } catch (Exception e) {
                    log.debug("Failed to parse trade date: {}", dateStr);
                    continue;
                }
                daily.setOpen(safeDecimal(node, "开盘价"));
                daily.setHigh(safeDecimal(node, "最高价"));
                daily.setLow(safeDecimal(node, "最低价"));
                daily.setClose(safeDecimal(node, "收盘价"));
                daily.setVolume(safeLong(node, "成交量"));
                daily.setAmount(safeDecimal(node, "成交额"));
                result.add(daily);
            }
        } catch (Exception e) {
            log.error("Failed to parse sector index daily JSON for {}/{}", sectorType, sectorCode, e);
            throw new RuntimeException("解析板块指数日线数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private void computeChangePct(List<SectorIndexDaily> entities, String sectorType, String sectorCode) {
        if (entities.isEmpty()) return;

        LocalDate minDate = entities.stream().map(SectorIndexDaily::getTradeDate).min(LocalDate::compareTo).orElse(null);
        if (minDate == null) return;

        List<SectorIndexDaily> existing = sectorIndexDailyMapper.selectList(
                new LambdaQueryWrapper<SectorIndexDaily>()
                        .eq(SectorIndexDaily::getSectorType, sectorType)
                        .eq(SectorIndexDaily::getSectorCode, sectorCode)
                        .lt(SectorIndexDaily::getTradeDate, minDate)
                        .orderByDesc(SectorIndexDaily::getTradeDate)
        );

        Map<LocalDate, SectorIndexDaily> allByDate = existing.stream()
                .collect(Collectors.toMap(SectorIndexDaily::getTradeDate, e -> e, (a, b) -> a));

        for (SectorIndexDaily e : entities) {
            allByDate.put(e.getTradeDate(), e);
        }

        for (SectorIndexDaily entity : entities) {
            SectorIndexDaily prev = allByDate.get(entity.getTradeDate().minusDays(1));
            if (prev != null && prev.getClose() != null && entity.getClose() != null
                    && prev.getClose().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal changePct = entity.getClose().subtract(prev.getClose())
                        .divide(prev.getClose(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                entity.setChangePct(changePct);
            }
        }
    }

    private int saveBatch(List<SectorIndexDaily> entities, String sectorType, String sectorCode) {
        List<SectorIndexDaily> existing = sectorIndexDailyMapper.selectList(
                new LambdaQueryWrapper<SectorIndexDaily>()
                        .eq(SectorIndexDaily::getSectorType, sectorType)
                        .eq(SectorIndexDaily::getSectorCode, sectorCode)
        );

        int count = partitionAndSave(entities, existing,
                SectorIndexDaily::getTradeDate, (src, tgt) -> tgt.setId(src.getId()), batchSqlRunner);

        log.info("SectorIndexDaily cleanse complete: {} records for {}/{}", count, sectorType, sectorCode);
        return count;
    }

}
