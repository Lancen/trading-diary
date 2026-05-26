package com.tradingdiary.collection.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.MarketDataService;
import com.tradingdiary.service.collection.ConstituentScrapeService;
import com.tradingdiary.service.market.SectorStockItem;
import com.tradingdiary.service.market.SectorStockService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/market")
@PreAuthorize("hasRole('ADMIN')")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final SectorStockService sectorStockService;
    private final ConstituentScrapeService constituentScrapeService;

    public MarketDataController(MarketDataService marketDataService,
                                 SectorStockService sectorStockService,
                                 ConstituentScrapeService constituentScrapeService) {
        this.marketDataService = marketDataService;
        this.sectorStockService = sectorStockService;
        this.constituentScrapeService = constituentScrapeService;
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

    @Operation(summary = "抓取指定板块的成分股（从同花顺网页）")
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
            Map<String, Object> result = constituentScrapeService.scrapeAndImport(type, code);
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(500, "抓取失败: " + e.getMessage());
        }
    }
}
