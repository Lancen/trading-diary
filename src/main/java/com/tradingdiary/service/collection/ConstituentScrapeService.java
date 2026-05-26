package com.tradingdiary.service.collection;

import java.util.Map;

public interface ConstituentScrapeService {

    Map<String, Object> scrapeAndImport(String boardType, String code);
}
