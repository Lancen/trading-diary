package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.MarginMacro;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.service.collection.MarginMacroCleanseService;
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
 * 两融总量清洗服务实现，解析市场级融资融券汇总 JSON 并入库
 */
@Service
public class MarginMacroCleanseServiceImpl implements MarginMacroCleanseService {

    private static final Logger log = LoggerFactory.getLogger(MarginMacroCleanseServiceImpl.class);

    private final MarginMacroMapper mapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public MarginMacroCleanseServiceImpl(MarginMacroMapper mapper, BatchSqlRunner batchSqlRunner,
                                          ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int cleanse(String rawJson, String exchange) {
        List<MarginMacro> entities = parse(rawJson, exchange);
        if (entities.isEmpty()) {
            log.warn("No macro margin records parsed for {}", exchange);
            return 0;
        }

        List<MarginMacro> existing = mapper.selectList(
                new LambdaQueryWrapper<MarginMacro>().eq(MarginMacro::getExchange, exchange));
        int count = partitionAndSave(entities, existing,
                MarginMacro::getTradeDate, (src, tgt) -> tgt.setId(src.getId()), batchSqlRunner);

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
                catch (Exception e) { log.debug("跳过无效日期记录: {}", e.getMessage()); continue; }

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

}
