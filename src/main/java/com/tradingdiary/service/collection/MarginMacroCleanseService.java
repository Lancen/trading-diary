package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginMacro;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MarginMacroCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginMacroCleanseService.class);

    private final MarginMacroMapper mapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public MarginMacroCleanseService(MarginMacroMapper mapper, BatchSqlRunner batchSqlRunner,
                                      ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int cleanse(String rawJson, String exchange) {
        List<MarginMacro> entities = parse(rawJson, exchange);
        if (entities.isEmpty()) {
            log.warn("No macro margin records parsed for {}", exchange);
            return 0;
        }

        List<MarginMacro> existing = mapper.selectList(
                new LambdaQueryWrapper<MarginMacro>().eq(MarginMacro::getExchange, exchange));
        java.util.Map<LocalDate, MarginMacro> existingMap = new java.util.HashMap<>();
        for (MarginMacro e : existing) {
            existingMap.put(e.getTradeDate(), e);
        }

        List<MarginMacro> toInsert = new ArrayList<>();
        List<MarginMacro> toUpdate = new ArrayList<>();
        for (MarginMacro e : entities) {
            MarginMacro exist = existingMap.get(e.getTradeDate());
            if (exist != null) {
                e.setId(exist.getId());
                toUpdate.add(e);
            } else {
                toInsert.add(e);
            }
        }

        int count = 0;
        if (!toInsert.isEmpty()) count += batchSqlRunner.batchInsert(toInsert);
        if (!toUpdate.isEmpty()) count += batchSqlRunner.batchUpdate(toUpdate);

        log.info("MarginMacro cleanse complete: {} records for {}", count, exchange);
        return count;
    }

    private List<MarginMacro> parse(String rawJson, String exchange) {
        List<MarginMacro> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;

            for (JsonNode node : root) {
                String dateStr = safeText(node, "日期");
                if (dateStr == null) continue;
                LocalDate date;
                try { date = LocalDate.parse(dateStr.substring(0, 10)); }
                catch (Exception e) { continue; }

                MarginMacro m = new MarginMacro();
                m.setTradeDate(date);
                m.setExchange(exchange);
                m.setMarginBuy(safeDecimal(node, "融资买入额"));
                m.setMarginBalance(safeDecimal(node, "融资余额"));
                m.setShortSellVol(safeLong(node, "融券卖出量"));
                m.setShortRemainVol(safeLong(node, "融券余量"));
                m.setShortBalance(safeDecimal(node, "融券余额"));
                m.setTotalBalance(safeDecimal(node, "融资融券余额"));
                result.add(m);
            }
        } catch (Exception e) {
            log.error("Failed to parse macro margin JSON for {}", exchange, e);
            throw new RuntimeException("解析两融总量数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private String safeText(JsonNode node, String field) {
        JsonNode fn = node.get(field);
        return (fn == null || fn.isNull()) ? null : fn.asText();
    }

    private BigDecimal safeDecimal(JsonNode node, String field) {
        JsonNode fn = node.get(field);
        if (fn == null || fn.isNull()) return null;
        try {
            String text = fn.asText();
            if (text == null || text.isEmpty() || "None".equals(text)) return null;
            return new BigDecimal(text);
        } catch (Exception e) { return null; }
    }

    private Long safeLong(JsonNode node, String field) {
        JsonNode fn = node.get(field);
        if (fn == null || fn.isNull()) return null;
        try {
            String text = fn.asText();
            if (text == null || text.isEmpty() || "None".equals(text)) return null;
            return Long.parseLong(text);
        } catch (Exception e) { return null; }
    }
}
