package com.tradingdiary.controller;

import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.service.market.SectorCorrelation;
import com.tradingdiary.service.market.SectorCorrelationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 板块关联度控制器，提供板块间相关性指标查询
 */
@RestController
@RequestMapping("/api/v1/admin/sector-correlation")
@PreAuthorize("hasRole('ADMIN')")
public class SectorCorrelationController {

    private final SectorCorrelationService sectorCorrelationService;

    public SectorCorrelationController(SectorCorrelationService sectorCorrelationService) {
        this.sectorCorrelationService = sectorCorrelationService;
    }

    @Operation(summary = "查询板块关联度（Jaccard相似度）")
    @GetMapping
    public ApiResponse<List<SectorCorrelation>> query(
            @RequestParam String sectorType,
            @RequestParam String sectorCode) {
        try {
            List<SectorCorrelation> result = sectorCorrelationService.compute(sectorType, sectorCode);
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
