package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.collection.TradeCalendarService;
import com.tradingdiary.util.BatchSqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 交易日历服务实现，从 API 同步交易日历数据到数据库
 */
@Service
public class TradeCalendarServiceImpl implements TradeCalendarService {

    private static final Logger log = LoggerFactory.getLogger(TradeCalendarServiceImpl.class);

    private final AKToolsClient aktoolsClient;
    private final TradeCalendarMapper tradeCalendarMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public TradeCalendarServiceImpl(AKToolsClient aktoolsClient,
                                     TradeCalendarMapper tradeCalendarMapper,
                                     BatchSqlRunner batchSqlRunner,
                                     ObjectMapper objectMapper) {
        this.aktoolsClient = aktoolsClient;
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    public int syncTradeCalendar() {
        log.info("Starting trade calendar sync");

        String responseJson = aktoolsClient.fetchTradeCalendar();

        List<LocalDate> fetchedDates = parseTradeDates(responseJson);

        Set<LocalDate> existingDates = tradeCalendarMapper.selectList(
                new LambdaQueryWrapper<TradeCalendar>()
                        .select(TradeCalendar::getTradeDate)
        ).stream()
                .map(TradeCalendar::getTradeDate)
                .collect(Collectors.toSet());

        List<TradeCalendar> newEntries = new ArrayList<>();
        for (LocalDate date : fetchedDates) {
            if (!existingDates.contains(date)) {
                TradeCalendar entry = new TradeCalendar();
                entry.setTradeDate(date);
                entry.setIsTradingDay(1);
                newEntries.add(entry);
            }
        }

        if (!newEntries.isEmpty()) {
            batchSqlRunner.batchInsert(newEntries);
        }

        log.info("Trade calendar sync complete: {} new dates added", newEntries.size());
        return newEntries.size();
    }

    private List<LocalDate> parseTradeDates(String responseJson) {
        List<LocalDate> dates = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    JsonNode dateNode = node.get("trade_date");
                    if (dateNode != null && !dateNode.isNull()) {
                        String dateStr = dateNode.asText();
                        try {
                            dates.add(LocalDate.parse(dateStr.substring(0, 10)));
                        } catch (Exception e) {
                            log.warn("Failed to parse trade date: {}", dateStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse trade calendar response", e);
            throw new RuntimeException("解析交易日历数据失败: " + e.getMessage(), e);
        }

        log.info("Parsed {} trade dates from response", dates.size());
        return dates;
    }
}
