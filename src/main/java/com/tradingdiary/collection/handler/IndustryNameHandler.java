package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.service.collection.IndustryCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 行业板块名称采集处理器
 */
@Component
public class IndustryNameHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final IndustryCleanseService industryCleanseService;

    public IndustryNameHandler(AKToolsClient aktoolsClient, IndustryCleanseService industryCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.industryCleanseService = industryCleanseService;
    }

    @Override
    public String dataType() {
        return "INDUSTRY_NAME";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        return FetchResult.single(aktoolsClient.fetchIndustryNames());
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return industryCleanseService.cleanseNames(rawJson);
    }
}