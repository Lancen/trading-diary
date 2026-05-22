package com.tradingdiary.collection.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
public class TushareClient {

    private static final Logger log = LoggerFactory.getLogger(TushareClient.class);
    private static final String BASE_URL = "https://api.tushare.pro";

    private final String token;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TushareClient(@Value("${app.tushare.secret}") String token,
                          RestClient.Builder builder,
                          ObjectMapper objectMapper) {
        this.token = token;
        this.restClient = builder.baseUrl(BASE_URL).build();
        this.objectMapper = objectMapper;
    }

    /**
     * 拉取全市场日线行情
     * <p>
     * 按 trade_date 一次拉取全市场 ~5500 只股票的 OHLCV 数据。
     * 返回原始 Tushare JSON（含 fields + items 二维数组）。
     * </p>
     */
    public String fetchDaily(LocalDate tradeDate) {
        String dateStr = tradeDate.toString().replace("-", "");
        log.info("Fetching Tushare daily: tradeDate={}", dateStr);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("api_name", "daily");
        body.put("token", token);
        ObjectNode params = body.putObject("params");
        params.put("trade_date", dateStr);
        body.put("fields", "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount");

        try {
            String resp = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(resp);
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                String msg = root.path("msg").asText("");
                throw new RuntimeException("Tushare daily API error: " + msg);
            }

            JsonNode data = root.path("data");
            ArrayNode items = (ArrayNode) data.path("items");
            log.info("Tushare daily fetched: tradeDate={}, records={}", dateStr, items.size());
            return resp;
        } catch (Exception e) {
            log.error("Tushare daily fetch failed for {}", dateStr, e);
            throw new RuntimeException("Tushare 日线拉取失败: " + e.getMessage(), e);
        }
    }
}
