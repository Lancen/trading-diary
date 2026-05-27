package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.MarginMacroCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 两融总量（沪市）采集处理器
 */
@Component
public class MarginMacroSseHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final MarginMacroCleanseService marginMacroCleanseService;

    public MarginMacroSseHandler(AKToolsClient aktoolsClient, MarginMacroCleanseService marginMacroCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.marginMacroCleanseService = marginMacroCleanseService;
    }

    @Override
    public String dataType() {
        return "MARGIN_MACRO_SSE";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        return aktoolsClient.fetchMacroMarginSh();
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return marginMacroCleanseService.cleanse(rawJson, "SSE");
    }
}