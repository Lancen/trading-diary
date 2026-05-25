package com.tradingdiary.collection.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.MarketDataService;
import com.tradingdiary.service.market.SectorStockItem;
import com.tradingdiary.service.market.SectorStockService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public MarketDataController(MarketDataService marketDataService, SectorStockService sectorStockService) {
        this.marketDataService = marketDataService;
        this.sectorStockService = sectorStockService;
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
}
