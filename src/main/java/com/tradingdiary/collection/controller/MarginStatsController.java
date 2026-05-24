package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.MarginSummaryVO;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.collection.MarginStatsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/margin-stats")
@PreAuthorize("hasRole('ADMIN')")
public class MarginStatsController {

    private final MarginStatsService marginStatsService;

    public MarginStatsController(MarginStatsService marginStatsService) {
        this.marginStatsService = marginStatsService;
    }

    @Operation(summary = "获取融资统计总量")
    @GetMapping("/summary")
    public ApiResponse<MarginSummaryVO> summary(@RequestParam(required = false) LocalDate tradeDate) {
        MarginSummaryVO vo = marginStatsService.getMarginSummary(tradeDate);
        return ApiResponse.ok(vo);
    }
}
