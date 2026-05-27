package com.tradingdiary.collection.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.MarketDataService;
import com.tradingdiary.service.collection.ConstituentScrapeService;
import com.tradingdiary.service.market.PinService;
import com.tradingdiary.service.market.SectorStockItem;
import com.tradingdiary.service.market.SectorStockService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 市场数据控制器，提供宽基指数行情查询
 */
@RestController
@RequestMapping("/api/v1/admin/market")
@PreAuthorize("hasRole('ADMIN')")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final SectorStockService sectorStockService;
    private final ConstituentScrapeService constituentScrapeService;
    private final PinService pinService;

    public MarketDataController(MarketDataService marketDataService,
                                 SectorStockService sectorStockService,
                                 ConstituentScrapeService constituentScrapeService,
                                 PinService pinService) {
        this.marketDataService = marketDataService;
        this.sectorStockService = sectorStockService;
        this.constituentScrapeService = constituentScrapeService;
        this.pinService = pinService;
    }

    @Operation(summary = "获取概念列表（含两融聚合）")
    @GetMapping("/concepts")
    public ApiResponse<Map<String, Object>> concepts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate tradeDate,
            @RequestParam(defaultValue = "marginBalance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(marketDataService.listConcepts(keyword, tradeDate, sortBy, sortDir, page, size));
    }

    @Operation(summary = "获取行业列表（含两融聚合）")
    @GetMapping("/industries")
    public ApiResponse<Map<String, Object>> industries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate tradeDate,
            @RequestParam(defaultValue = "marginBalance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(marketDataService.listIndustries(keyword, tradeDate, sortBy, sortDir, page, size));
    }

    @Operation(summary = "获取行业成分股列表")
    @GetMapping("/industries/{code}/stocks")
    public ApiResponse<List<SectorStockItem>> industryStocks(@PathVariable String code) {
        return ApiResponse.ok(sectorStockService.listIndustryStocks(code));
    }

    @Operation(summary = "获取概念成分股列表")
    @GetMapping("/concepts/{code}/stocks")
    public ApiResponse<List<SectorStockItem>> conceptStocks(@PathVariable String code) {
        return ApiResponse.ok(sectorStockService.listConceptStocks(code));
    }

    @Operation(summary = "抓取指定板块的成分股（异步）")
    @PostMapping("/constituents/scrape")
    public ApiResponse<Map<String, Object>> scrapeConstituents(
            @RequestParam String type,
            @RequestParam String code) {
        if (!"industry".equals(type) && !"concept".equals(type)) {
            return ApiResponse.fail(400, "type 仅支持 industry 或 concept");
        }
        if (code == null || code.isBlank()) {
            return ApiResponse.fail(400, "板块代码不能为空");
        }
        try {
            constituentScrapeService.startAsyncScrape(type, code);
            return ApiResponse.ok(Map.of("status", "scraping", "boardType", type, "code", code));
        } catch (Exception e) {
            return ApiResponse.fail(500, "启动抓取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询成分股抓取状态")
    @GetMapping("/constituents/scrape/status")
    public ApiResponse<Map<String, Object>> scrapeStatus(
            @RequestParam String type,
            @RequestParam String code) {
        if (!"industry".equals(type) && !"concept".equals(type)) {
            return ApiResponse.fail(400, "type 仅支持 industry 或 concept");
        }
        return ApiResponse.ok(constituentScrapeService.getScrapeStatus(type, code));
    }

    @Operation(summary = "置顶/取消置顶行业或概念")
    @PostMapping("/pin")
    public ApiResponse<Map<String, Object>> togglePin(@RequestBody Map<String, String> body) {
        String type = body.get("type");
        String code = body.get("code");
        Boolean pinned = Boolean.valueOf(body.getOrDefault("pinned", "true"));
        if (!"industry".equals(type) && !"concept".equals(type)) {
            return ApiResponse.fail(400, "type 仅支持 industry 或 concept");
        }
        if (code == null || code.isBlank()) {
            return ApiResponse.fail(400, "板块代码不能为空");
        }
        return ApiResponse.ok(pinService.togglePin(type, code, pinned));
    }

    @Operation(summary = "调整置顶排序")
    @PostMapping("/pin/reorder")
    public ApiResponse<Map<String, Object>> reorderPin(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) body.get("codes");
        if (!"industry".equals(type) && !"concept".equals(type)) {
            return ApiResponse.fail(400, "type 仅支持 industry 或 concept");
        }
        if (codes == null || codes.isEmpty()) {
            return ApiResponse.fail(400, "codes 不能为空");
        }
        return ApiResponse.ok(pinService.reorderPinned(type, codes));
    }
}
