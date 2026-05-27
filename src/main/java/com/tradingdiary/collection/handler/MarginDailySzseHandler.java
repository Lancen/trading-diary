package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.MarginCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 两融明细（深市）采集处理器
 */
@Component
public class MarginDailySzseHandler implements DataTypeHandler {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AKToolsClient aktoolsClient;
    private final MarginCleanseService marginCleanseService;

    public MarginDailySzseHandler(AKToolsClient aktoolsClient, MarginCleanseService marginCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.marginCleanseService = marginCleanseService;
    }

    @Override
    public String dataType() {
        return "MARGIN_DAILY_SZSE";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        String dateStr = tradeDate != null ? tradeDate.format(YMD) : "";
        return FetchResult.single(aktoolsClient.fetchMarginDetailSzse(dateStr));
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return marginCleanseService.cleanse(rawJson, "SZSE", tradeDate);
    }
}