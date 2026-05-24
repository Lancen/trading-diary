package com.tradingdiary.collection.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AKTools API 客户端，封装东方财富数据接口的 HTTP 调用与限流逻辑
 */
@Component
public class AKToolsClient {

    private static final Logger log = LoggerFactory.getLogger(AKToolsClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    /**
     * AKTools API 调用最小间隔（毫秒），避免请求过于频繁。
     */
    private static final long RATE_LIMIT_DELAY_MS = 200;

    /**
     * 上次 API 调用的时间戳，用于限流。
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
        String prefixed = toTxSymbol(symbol);
        log.info("Fetching stock daily data (腾讯): symbol={}, startDate={}, endDate={}", prefixed, startDate, endDate);
        return get("/api/public/stock_zh_a_hist_tx?symbol={symbol}&start_date={startDate}&end_date={endDate}",
                prefixed, startDate, endDate);
    }

    private static String toTxSymbol(String symbol) {
        char first = symbol.charAt(0);
        if (first == '6') return "sh" + symbol;  // 上交所
        if (first == '0' || first == '3') return "sz" + symbol;  // 深交所
        return "nq" + symbol;  // 北交所/新三板 (4/8/9)
    }

    public String fetchIndustryNames() {
        log.info("Fetching industry board names (同花顺)");
        return get("/api/public/stock_board_industry_name_ths");
    }

    public String fetchConceptNames() {
        log.info("Fetching concept board names (同花顺)");
        return get("/api/public/stock_board_concept_name_ths");
    }

    public String fetchMacroMarginSh() {
        log.info("Fetching macro margin SSE");
        return get("/api/public/macro_china_market_margin_sh");
    }

    public String fetchMacroMarginSz() {
        log.info("Fetching macro margin SZSE");
        return get("/api/public/macro_china_market_margin_sz");
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
     * 执行 AKTools API 调用之间的限流。
     * 确保连续调用之间至少间隔 RATE_LIMIT_DELAY_MS 毫秒。
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
     * 批量顺序调用时的间隔等待。
     */
    public void sleepBetweenCalls() {
        // get() 中的 rateLimit() 已处理单次调用间隔，
        // 此方法确保批量循环中的额外间隔
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
