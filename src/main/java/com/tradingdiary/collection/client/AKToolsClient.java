package com.tradingdiary.collection.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AKToolsClient {

    private static final Logger log = LoggerFactory.getLogger(AKToolsClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    /**
     * Minimum delay between AKTools API calls to avoid overwhelming the service (in milliseconds).
     */
    private static final long RATE_LIMIT_DELAY_MS = 200;

    /**
     * Timestamp of the last API call, used for rate limiting.
     */
    private volatile long lastCallTimestamp = 0;

    public AKToolsClient(@Value("${aktools.base-url:http://localhost:8081}") String baseUrl) {
        this.baseUrl = baseUrl;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(this.baseUrl)
                .build();
    }

    public String fetchStockSpot() {
        log.info("Fetching stock spot data from AKTools (新浪)");
        return get("/api/public/stock_zh_a_spot");
    }

    public String fetchStockDaily(String symbol, String startDate, String endDate) {
        log.info("Fetching stock daily data (腾讯): symbol={}, startDate={}, endDate={}", symbol, startDate, endDate);
        return get("/api/public/stock_zh_a_hist_tx?symbol={symbol}&start_date={startDate}&end_date={endDate}",
                symbol, startDate, endDate);
    }

    public String fetchIndustryNames() {
        log.info("Fetching industry board names (同花顺)");
        return get("/api/public/stock_board_industry_name_ths");
    }

    /**
     * @deprecated 东方财富接口被封，行业成分股改用同花顺 Playwright 抓取
     */
    @Deprecated
    public String fetchIndustryCons(String symbol) {
        log.warn("fetchIndustryCons is deprecated — 行业成分股已改为 Playwright 抓取同花顺");
        return "[]";
    }

    public String fetchConceptNames() {
        log.info("Fetching concept board names (同花顺)");
        return get("/api/public/stock_board_concept_name_ths");
    }

    /**
     * @deprecated 东方财富接口被封，概念成分股改用同花顺 Playwright 抓取
     */
    @Deprecated
    public String fetchConceptCons(String symbol) {
        log.warn("fetchConceptCons is deprecated — 概念成分股已改为 Playwright 抓取同花顺");
        return "[]";
    }

    public String fetchTradeCalendar() {
        log.info("Fetching trade calendar");
        return get("/api/public/tool_trade_date_hist_sina");
    }

    public String fetchMarginDetailSse(String date) {
        log.info("Fetching margin detail SSE: date={}", date);
        return get("/api/public/stock_margin_detail_sse?date={date}", date);
    }

    public String fetchMarginDetailSzse(String date) {
        log.info("Fetching margin detail SZSE: date={}", date);
        return get("/api/public/stock_margin_detail_szse?date={date}", date);
    }

    /**
     * @deprecated 东方财富接口被封，改用同花顺 Playwright 抓取
     */
    @Deprecated
    public List<String> fetchIndustryConsBatch(List<String> symbols) {
        log.warn("fetchIndustryConsBatch is deprecated");
        List<String> results = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i++) {
            results.add("[]");
        }
        return results;
    }

    /**
     * @deprecated 东方财富接口被封，改用同花顺 Playwright 抓取
     */
    @Deprecated
    public List<String> fetchConceptConsBatch(List<String> symbols) {
        log.warn("fetchConceptConsBatch is deprecated");
        List<String> results = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i++) {
            results.add("[]");
        }
        return results;
    }

    private String get(String path, Object... uriVariables) {
        rateLimit();
        try {
            return restClient.get()
                    .uri(path, uriVariables)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new RuntimeException("AKTools 请求失败 " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Enforce rate limiting between AKTools API calls.
     * Ensures a minimum delay of RATE_LIMIT_DELAY_MS between successive calls.
     */
    private void rateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTimestamp;

        if (elapsed < RATE_LIMIT_DELAY_MS) {
            try {
                Thread.sleep(RATE_LIMIT_DELAY_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastCallTimestamp = System.currentTimeMillis();
    }

    /**
     * Sleep between calls for rate limiting when doing sequential batch operations.
     */
    public void sleepBetweenCalls() {
        // rateLimit() in get() already handles per-call delay,
        // this ensures inter-call spacing in batch loops
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
