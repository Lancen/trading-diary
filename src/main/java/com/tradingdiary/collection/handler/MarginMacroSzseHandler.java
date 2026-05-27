package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.MarginMacroCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 两融总量（深市）采集处理器
 */
@Component
public class MarginMacroSzseHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final MarginMacroCleanseService marginMacroCleanseService;

    public MarginMacroSzseHandler(AKToolsClient aktoolsClient, MarginMacroCleanseService marginMacroCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.marginMacroCleanseService = marginMacroCleanseService;
    }

    @Override
    public String dataType() {
        return "MARGIN_MACRO_SZSE";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        return FetchResult.single(aktoolsClient.fetchMacroMarginSz());
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return marginMacroCleanseService.cleanse(rawJson, "SZSE");
    }
}