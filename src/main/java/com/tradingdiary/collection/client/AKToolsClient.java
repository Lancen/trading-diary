package com.tradingdiary.collection.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class AKToolsClient {

    private static final Logger log = LoggerFactory.getLogger(AKToolsClient.class);

    private final RestClient restClient;
    private final String baseUrl;

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
        log.info("Fetching stock spot data from AKTools");
        return get("/api/public/stock_zh_a_spot_em");
    }

    public String fetchStockDaily(String symbol, String startDate, String endDate) {
        log.info("Fetching stock daily data: symbol={}, startDate={}, endDate={}", symbol, startDate, endDate);
        return get("/api/public/stock_zh_a_hist?symbol={symbol}&period=daily&start_date={startDate}&end_date={endDate}&adjust=qfq",
                symbol, startDate, endDate);
    }

    public String fetchIndustryNames() {
        log.info("Fetching industry board names");
        return get("/api/public/stock_board_industry_name_em");
    }

    public String fetchIndustryCons(String symbol) {
        log.info("Fetching industry constituents: symbol={}", symbol);
        return get("/api/public/stock_board_industry_cons_em?symbol={symbol}", symbol);
    }

    public String fetchConceptNames() {
        log.info("Fetching concept board names");
        return get("/api/public/stock_board_concept_name_em");
    }

    public String fetchConceptCons(String symbol) {
        log.info("Fetching concept constituents: symbol={}", symbol);
        return get("/api/public/stock_board_concept_cons_em?symbol={symbol}", symbol);
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
        return restClient.get()
                .uri(path, uriVariables)
                .retrieve()
                .body(String.class);
    }
}
