package com.tradingdiary.collection.controller;

import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.SectorIndexDailyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 板块指数日线控制器，提供行业/概念指数日线数据查询
 */
@RestController
@RequestMapping("/api/v1/admin/sector-index-daily")
@PreAuthorize("hasRole('ADMIN')")
public class SectorIndexDailyController {

    private final SectorIndexDailyService sectorIndexDailyService;

    public SectorIndexDailyController(SectorIndexDailyService sectorIndexDailyService) {
        this.sectorIndexDailyService = sectorIndexDailyService;
    }

    @Operation(summary = "查询板块指数日线数据")
    @GetMapping
    public ApiResponse<List<SectorIndexDaily>> query(
            @RequestParam String sectorType,
            @RequestParam String sectorCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        if (!SectorIndexDailyService.VALID_SECTOR_TYPES.contains(sectorType)) {
            return ApiResponse.fail(400, "Invalid sectorType: " + sectorType + ", must be INDUSTRY or CONCEPT");
        }
        return ApiResponse.ok(sectorIndexDailyService.query(sectorType, sectorCode, startDate, endDate));
    }
}