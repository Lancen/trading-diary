package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.StockDetailVO;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.StockDataService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stocks")
@PreAuthorize("hasRole('ADMIN')")
public class StockDataController {

    private final StockDataService stockDataService;

    public StockDataController(StockDataService stockDataService) {
        this.stockDataService = stockDataService;
    }

    @Operation(summary = "获取股票列表（含行情和两融数据）")
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String concept,
            @RequestParam(required = false) LocalDate tradeDate,
            @RequestParam(defaultValue = "changePct") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Map<String, Object> result = stockDataService.listStocks(
                keyword, industry, concept, tradeDate, sortBy, sortDir, page, size);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "获取单只股票详情（日线+两融+行业概念）")
    @GetMapping("/{code}")
    public ApiResponse<StockDetailVO> detail(
            @PathVariable String code,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        StockDetailVO detail = stockDataService.getStockDetail(code, startDate, endDate);
        return ApiResponse.ok(detail);
    }
}
