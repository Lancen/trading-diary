package com.tradingdiary.collection.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.MacroMarginDaily;
import com.tradingdiary.service.market.MacroMarginService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 两融总量控制器，提供市场级融资融券汇总数据查询
 */
@RestController
@RequestMapping("/api/v1/admin/margin-macro")
@PreAuthorize("hasRole('ADMIN')")
public class MarginMacroController {

    private final MacroMarginService macroMarginService;

    public MarginMacroController(MacroMarginService macroMarginService) {
        this.macroMarginService = macroMarginService;
    }

    @Operation(summary = "查询全市场两融聚合数据")
    @GetMapping("/sse")
    public ApiResponse<List<MacroMarginDaily>> query(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<MacroMarginDaily> records = macroMarginService.aggregate(startDate, endDate);
        return ApiResponse.ok(records);
    }
}
