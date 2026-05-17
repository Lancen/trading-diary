package com.tradingdiary.service.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TradeCalendarService {

    private static final Logger log = LoggerFactory.getLogger(TradeCalendarService.class);

    private final AKToolsClient aktoolsClient;
    private final TradeCalendarMapper tradeCalendarMapper;
    private final ObjectMapper objectMapper;

    public TradeCalendarService(AKToolsClient aktoolsClient,
                                TradeCalendarMapper tradeCalendarMapper,
                                ObjectMapper objectMapper) {
        this.aktoolsClient = aktoolsClient;
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.objectMapper = objectMapper;
    }

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
            for (TradeCalendar entry : newEntries) {
                tradeCalendarMapper.insert(entry);
            }
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
                        // 兼容两种格式: "yyyy-MM-dd" 和 "yyyy-MM-ddTHH:mm:ss.SSS"
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
            throw new RuntimeException("Failed to parse trade calendar data: " + e.getMessage(), e);
        }

        log.info("Parsed {} trade dates from response", dates.size());
        return dates;
    }
}
