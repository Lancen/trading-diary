package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.service.collection.MarginCleanseService;
import com.tradingdiary.util.BatchSqlRunner;
import com.tradingdiary.util.JsonNodeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tradingdiary.util.JsonNodeHelper.*;
import static com.tradingdiary.util.UpsertHelper.partitionAndSave;

/**
 * 两融明细清洗服务实现，解析个股融资融券 JSON 并入库
 */
@Service
public class MarginCleanseServiceImpl implements MarginCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginCleanseServiceImpl.class);

    private final MarginDailyMapper marginDailyMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public MarginCleanseServiceImpl(MarginDailyMapper marginDailyMapper,
                                     BatchSqlRunner batchSqlRunner, ObjectMapper objectMapper) {
        this.marginDailyMapper = marginDailyMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int cleanse(String rawJson, String exchange, LocalDate tradeDate) {
        List<MarginDaily> dailyList = parseMarginDailyList(rawJson, exchange, tradeDate);
        if (dailyList.isEmpty()) {
            log.warn("No margin daily records parsed for {} on {}", exchange, tradeDate);
            return 0;
        }

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

        int count = partitionAndSave(dailyList, existing,
                MarginDaily::getStockCode, (src, tgt) -> tgt.setId(src.getId()), batchSqlRunner);
        return count;
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
                String rawCode = isSZSE ? safeText(node, "证券代码") : safeText(node, "标的证券代码");
                String stockCode = stripMarketPrefix(rawCode);
                daily.setStockCode(stockCode);
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
}
