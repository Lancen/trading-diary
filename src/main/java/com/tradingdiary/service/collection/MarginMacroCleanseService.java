package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.CollectionConstants;
import com.tradingdiary.entity.MarginMacro;
import com.tradingdiary.mapper.MarginMacroMapper;
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

@Service
public class MarginMacroCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginMacroCleanseService.class);

    private final MarginMacroMapper mapper;
    private final ObjectMapper objectMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public MarginMacroCleanseService(MarginMacroMapper mapper, ObjectMapper objectMapper,
                                      SqlSessionFactory sqlSessionFactory) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Transactional
    public int cleanse(String rawJson, String exchange) {
        List<MarginMacro> entities = parse(rawJson, exchange);
        if (entities.isEmpty()) {
            log.warn("No macro margin records parsed for {}", exchange);
            return 0;
        }

        int count = 0;
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            MarginMacroMapper bm = session.getMapper(MarginMacroMapper.class);
            for (int i = 0; i < entities.size(); i++) {
                MarginMacro e = entities.get(i);
                List<MarginMacro> existing = bm.selectList(
                        new LambdaQueryWrapper<MarginMacro>()
                                .eq(MarginMacro::getTradeDate, e.getTradeDate())
                                .eq(MarginMacro::getExchange, e.getExchange())
                );
                if (!existing.isEmpty()) {
                    e.setId(existing.get(0).getId());
                    bm.updateById(e);
                } else {
                    bm.insert(e);
                }
                count++;
                if ((i + 1) % CollectionConstants.DB_BATCH_SIZE == 0) {
                    session.flushStatements();
                }
            }
            session.flushStatements();
        }

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
