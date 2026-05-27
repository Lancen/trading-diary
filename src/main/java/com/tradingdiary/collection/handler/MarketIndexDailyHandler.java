package com.tradingdiary.collection.handler;

import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.service.collection.MarketIndexDailyCleanseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 宽基指数日线采集处理器，内部遍历 8 个指数逐一采集和清洗
 */
@Component
public class MarketIndexDailyHandler implements DataTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketIndexDailyHandler.class);

    private static final List<String> MARKET_INDEX_CODES = List.of(
            "sh000001", "sz399001", "sz399006", "sh000300", "sh000905",
            "sh000016", "sh000688", "sh000852"
    );

    private final AKToolsClient aktoolsClient;
    private final MarketIndexDailyCleanseService marketIndexDailyCleanseService;

    public MarketIndexDailyHandler(AKToolsClient aktoolsClient,
                                   MarketIndexDailyCleanseService marketIndexDailyCleanseService) {
        this.aktoolsClient = aktoolsClient;
        this.marketIndexDailyCleanseService = marketIndexDailyCleanseService;
    }

    @Override
    public String dataType() {
        return "MARKET_INDEX_DAILY";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        StringBuilder combined = new StringBuilder("[");
        for (int i = 0; i < MARKET_INDEX_CODES.size(); i++) {
            String code = MARKET_INDEX_CODES.get(i);
            try {
                String json = aktoolsClient.fetchMarketIndexDaily(code);
                if (json != null && !json.equals("[]")) {
                    if (combined.length() > 1) combined.append(",");
                    combined.append("\"").append(code).append("\":").append(json);
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to fetch market index daily for {}: {}", code, e.getMessage());
            }
        }
        combined.append("]");
        return combined.toString();
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        int totalRecords = 0;
        for (String indexCode : MARKET_INDEX_CODES) {
            try {
                String perIndexJson = aktoolsClient.fetchMarketIndexDaily(indexCode);
                if (perIndexJson != null && !perIndexJson.equals("[]")) {
                    int count = marketIndexDailyCleanseService.cleanse(perIndexJson, indexCode);
                    totalRecords += count;
                    log.info("Market index daily cleanse: {} → {} records", indexCode, count);
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to cleanse market index daily for {}: {}", indexCode, e.getMessage());
            }
        }
        return totalRecords;
    }
}