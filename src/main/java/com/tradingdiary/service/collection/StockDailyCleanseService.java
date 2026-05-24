package com.tradingdiary.service.collection;

import com.tradingdiary.entity.StockDaily;

import java.time.LocalDate;
import java.util.List;

public interface StockDailyCleanseService {

    int cleanse(String rawJson, LocalDate tradeDate);

    int cleanseHistBatch(List<String> rawJsonList, List<String> stockCodes);

    int cleanseHistJson(String rawJson, String stockCode);

    int cleanseTushareDaily(String tushareResponse);

    List<StockDaily> parseTushareDaily(String tushareResponse);
}
