package com.tradingdiary.service.collection;

public interface MarketIndexDailyCleanseService {

    int cleanse(String rawJson, String indexCode);
}
