package com.tradingdiary.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.model.vo.SectorRanking;
import com.tradingdiary.service.market.SectorRankingService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 板块排名控制器，提供行业/概念板块排名查询
 */
@RestController
@RequestMapping("/api/v1/admin/sector-ranking")
@PreAuthorize("hasRole('ADMIN')")
public class SectorRankingController {

    private final SectorRankingService sectorRankingService;

    public SectorRankingController(SectorRankingService sectorRankingService) {
        this.sectorRankingService = sectorRankingService;
    }

    @Operation(summary = "查询板块排名")
    @GetMapping
    public ApiResponse<List<SectorRanking>> query(
            @RequestParam String sectorType,
            @RequestParam(required = false) LocalDate tradeDate,
            @RequestParam(defaultValue = "amount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        if (!SectorRankingService.VALID_TYPES.contains(sectorType)) {
            return ApiResponse.fail(400, "Invalid sectorType: " + sectorType);
        }
        return ApiResponse.ok(sectorRankingService.query(sectorType, tradeDate, sortBy, sortDir));
    }
}