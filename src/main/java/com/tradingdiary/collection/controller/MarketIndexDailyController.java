package com.tradingdiary.collection.controller;

import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.MarketIndexDailyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/market-index-daily")
@PreAuthorize("hasRole('ADMIN')")
public class MarketIndexDailyController {

    private final MarketIndexDailyService marketIndexDailyService;

    public MarketIndexDailyController(MarketIndexDailyService marketIndexDailyService) {
        this.marketIndexDailyService = marketIndexDailyService;
    }

    @Operation(summary = "查询宽基指数日线数据")
    @GetMapping
    public ApiResponse<List<MarketIndexDaily>> query(
            @RequestParam String indexCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<MarketIndexDaily> records = marketIndexDailyService.query(indexCode, startDate, endDate);
        return ApiResponse.ok(records);
    }

    @Operation(summary = "查询所有已采集宽基指数的最新行情")
    @GetMapping("/latest")
    public ApiResponse<List<MarketIndexDaily>> latest() {
        List<MarketIndexDaily> records = marketIndexDailyService.latest();
        return ApiResponse.ok(records);
    }
}
