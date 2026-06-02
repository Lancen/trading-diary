package com.tradingdiary.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.CrowdednessService;
import com.tradingdiary.service.market.CrowdednessService.CrowdednessDaily;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 拥挤度控制器，提供市场拥挤度指标查询
 */
@RestController
@RequestMapping("/api/v1/admin/crowdedness")
@PreAuthorize("hasRole('ADMIN')")
public class CrowdednessController {

    private final CrowdednessService crowdednessService;

    public CrowdednessController(CrowdednessService crowdednessService) {
        this.crowdednessService = crowdednessService;
    }

    @Operation(summary = "查询拥挤度指标")
    @GetMapping
    public ApiResponse<List<CrowdednessDaily>> query(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ApiResponse.ok(crowdednessService.query(startDate, endDate));
    }
}
