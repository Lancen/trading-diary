package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.MarginCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 两融明细（沪市）采集处理器
 */
@Component
public class MarginDailySseHandler implements DataTypeHandler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AKToolsClient aktoolsClient;
    private final MarginCleanseService marginCleanseService;

    public MarginDailySseHandler(AKToolsClient aktoolsClient, MarginCleanseService marginCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.marginCleanseService = marginCleanseService;
    }

    @Override
    public String dataType() {
        return "MARGIN_DAILY_SSE";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        String dateStr = tradeDate != null ? tradeDate.format(YMD) : "";
        return FetchResult.single(aktoolsClient.fetchMarginDetailSse(dateStr));
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return marginCleanseService.cleanse(rawJson, "SSE", tradeDate);
    }
}