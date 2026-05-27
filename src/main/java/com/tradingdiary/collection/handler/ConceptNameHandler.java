package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.ConceptCleanseService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 概念板块名称采集处理器
 */
@Component
public class ConceptNameHandler implements DataTypeHandler {

    private final AKToolsClient aktoolsClient;
    private final ConceptCleanseService conceptCleanseService;

    public ConceptNameHandler(AKToolsClient aktoolsClient, ConceptCleanseService conceptCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.conceptCleanseService = conceptCleanseService;
    }

    @Override
    public String dataType() {
        return "CONCEPT_NAME";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        return aktoolsClient.fetchConceptNames();
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        return conceptCleanseService.cleanseNames(rawJson);
    }
}