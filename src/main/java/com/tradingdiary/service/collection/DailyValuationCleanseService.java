package com.tradingdiary.service.collection;

import com.tradingdiary.entity.StockDailyValuation;

import java.util.List;

public interface DailyValuationCleanseService {

    int cleanse(String rawJson);

    List<StockDailyValuation> parse(String rawJson);
}
