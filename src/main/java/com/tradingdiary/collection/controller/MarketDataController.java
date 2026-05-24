package com.tradingdiary.collection.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 市场数据控制器，提供概念和行业维度的两融聚合数据查询
 */
@RestController
@RequestMapping("/api/v1/admin/market")
@PreAuthorize("hasRole('ADMIN')")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
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
}
