package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockDailyValuation;
import com.tradingdiary.mapper.StockDailyValuationMapper;
import com.tradingdiary.service.collection.DailyValuationCleanseService;
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

import static com.tradingdiary.util.JsonNodeHelper.safeDecimal;
import static com.tradingdiary.util.JsonNodeHelper.safeText;
import static com.tradingdiary.util.UpsertHelper.partitionAndSave;

@Service
public class DailyValuationCleanseServiceImpl implements DailyValuationCleanseService {

    private static final Logger log = LoggerFactory.getLogger(DailyValuationCleanseServiceImpl.class);

    private final StockDailyValuationMapper valuationMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public DailyValuationCleanseServiceImpl(StockDailyValuationMapper valuationMapper,
                                             BatchSqlRunner batchSqlRunner,
                                             ObjectMapper objectMapper) {
        this.valuationMapper = valuationMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int cleanse(String rawJson) {
        List<StockDailyValuation> entities = parse(rawJson);
        if (entities.isEmpty()) return 0;

        LocalDate tradeDate = entities.get(0).getTradeDate();
        List<StockDailyValuation> existing = valuationMapper.selectList(
                new LambdaQueryWrapper<StockDailyValuation>()
                        .eq(StockDailyValuation::getTradeDate, tradeDate));

        int count = partitionAndSave(entities, existing,
                StockDailyValuation::getStockCode,
                (src, tgt) -> tgt.setId(src.getId()), batchSqlRunner);

        log.info("DailyValuation cleanse: {} records for {}", count, tradeDate);
        return count;
    }

    @Override
    public List<StockDailyValuation> parse(String rawJson) {
        List<StockDailyValuation> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;
            for (JsonNode node : root) {
                StockDailyValuation entity = new StockDailyValuation();
                String rawCode = safeText(node, "代码");
                if (rawCode == null) rawCode = safeText(node, "股票代码");
                entity.setStockCode(JsonNodeHelper.stripMarketPrefix(rawCode));
                entity.setTradeDate(LocalDate.now());
                entity.setPeTtm(safeDecimal(node, "市盈率-动态"));
                entity.setPeStatic(safeDecimal(node, "市盈率-静态"));
                entity.setPb(safeDecimal(node, "市净率"));
                entity.setPsTtm(safeDecimal(node, "市销率"));
                entity.setTotalMv(parseMarketValue(safeText(node, "总市值")));
                entity.setCircMv(parseMarketValue(safeText(node, "流通市值")));
                entity.setTurnoverRate(safeDecimal(node, "换手率"));
                if (entity.getStockCode() != null && !entity.getStockCode().isEmpty()) {
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse daily valuation JSON", e);
            throw new RuntimeException("解析每日估值数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private BigDecimal parseMarketValue(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            text = text.replaceAll(",", "");
            if (text.endsWith("亿")) {
                return new BigDecimal(text.replace("亿", "")).multiply(BigDecimal.valueOf(100000000));
            } else if (text.endsWith("万")) {
                return new BigDecimal(text.replace("万", "")).multiply(BigDecimal.valueOf(10000));
            }
            return new BigDecimal(text);
        } catch (Exception e) {
            return safeDecimalFromText(text);
        }
    }

    private BigDecimal safeDecimalFromText(String text) {
        try {
            return new BigDecimal(text.replaceAll("[^0-9.-]", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
