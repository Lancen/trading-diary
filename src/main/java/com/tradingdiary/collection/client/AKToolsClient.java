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

    /**
     * 拉取全市场 A 股实时行情（新浪数据源）
     *
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchStockSpot() {
        log.info("Fetching stock spot data from AKTools (新浪)");
        return get("/api/public/stock_zh_a_spot");
    }

    /**
     * 拉取同花顺行业板块名称列表
     *
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchIndustryNames() {
        log.info("Fetching industry board names (同花顺)");
        return get("/api/public/stock_board_industry_name_ths");
    }

    /**
     * 拉取同花顺概念板块名称列表
     *
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchConceptNames() {
        log.info("Fetching concept board names (同花顺)");
        return get("/api/public/stock_board_concept_name_ths");
    }

    /**
     * 拉取上海证券交易所宏观两融总量数据
     *
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchMacroMarginSh() {
        log.info("Fetching macro margin SSE");
        return get("/api/public/macro_china_market_margin_sh");
    }

    /**
     * 拉取深圳证券交易所宏观两融总量数据
     *
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchMacroMarginSz() {
        log.info("Fetching macro margin SZSE");
        return get("/api/public/macro_china_market_margin_sz");
    }

    /**
     * 拉取交易日历（新浪数据源）
     *
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchTradeCalendar() {
        log.info("Fetching trade calendar");
        return get("/api/public/tool_trade_date_hist_sina");
    }

    /**
     * 拉取上交所个股两融明细数据
     *
     * @param date 查询日期（格式 yyyyMMdd）
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchMarginDetailSse(String date) {
        log.info("Fetching margin detail SSE: date={}", date);
        return get("/api/public/stock_margin_detail_sse?date={date}", date);
    }

    /**
     * 拉取深交所个股两融明细数据
     *
     * @param date 查询日期（格式 yyyyMMdd）
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchMarginDetailSzse(String date) {
        log.info("Fetching margin detail SZSE: date={}", date);
        return get("/api/public/stock_margin_detail_szse?date={date}", date);
    }

    /**
     * 拉取宽基指数日线行情数据
     *
     * @param symbol 指数代码（如 sh000300）
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchMarketIndexDaily(String symbol) {
        log.info("Fetching market index daily: symbol={}", symbol);
        return get("/api/public/stock_zh_index_daily?symbol={symbol}", symbol);
    }

    /**
     * 拉取同花顺行业指数日线数据
     *
     * @param industryName 行业名称
     * @param startDate 起始日期（格式 yyyyMMdd）
     * @param endDate 结束日期（格式 yyyyMMdd）
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchIndustryIndexDaily(String industryName, String startDate, String endDate) {
        log.info("Fetching industry index daily: industry={}, startDate={}, endDate={}", industryName, startDate, endDate);
        return get("/api/public/stock_board_industry_index_ths?symbol={industryName}&start_date={startDate}&end_date={endDate}",
                industryName, startDate, endDate);
    }

    /**
     * 拉取同花顺概念指数日线数据
     *
     * @param conceptName 概念名称
     * @param startDate 起始日期（格式 yyyyMMdd）
     * @param endDate 结束日期（格式 yyyyMMdd）
     * @return AKTools 返回的原始 JSON 字符串
     */
    public String fetchConceptIndexDaily(String conceptName, String startDate, String endDate) {
        log.info("Fetching concept index daily: concept={}, startDate={}, endDate={}", conceptName, startDate, endDate);
        return get("/api/public/stock_board_concept_index_ths?symbol={conceptName}&start_date={startDate}&end_date={endDate}",
                conceptName, startDate, endDate);
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
