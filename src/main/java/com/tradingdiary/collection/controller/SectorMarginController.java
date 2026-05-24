package com.tradingdiary.collection.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.SectorMarginDaily;
import com.tradingdiary.service.market.SectorMarginService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sector-margin")
@PreAuthorize("hasRole('ADMIN')")
public class SectorMarginController {

    private final SectorMarginService sectorMarginService;

    public SectorMarginController(SectorMarginService sectorMarginService) {
        this.sectorMarginService = sectorMarginService;
    }

    @Operation(summary = "查询板块两融聚合数据")
    @GetMapping
    public ApiResponse<List<SectorMarginDaily>> query(
            @RequestParam String sectorType,
            @RequestParam String sectorCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<SectorMarginDaily> records = sectorMarginService.aggregate(sectorType, sectorCode, startDate, endDate);
        return ApiResponse.ok(records);
    }
}
