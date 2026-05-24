package com.tradingdiary.collection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.mapper.SectorIndexDailyMapper;
import com.tradingdiary.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sector-index-daily")
@PreAuthorize("hasRole('ADMIN')")
public class SectorIndexDailyController {

    private final SectorIndexDailyMapper sectorIndexDailyMapper;

    public SectorIndexDailyController(SectorIndexDailyMapper sectorIndexDailyMapper) {
        this.sectorIndexDailyMapper = sectorIndexDailyMapper;
    }

    @Operation(summary = "查询板块指数日线数据")
    @GetMapping
    public ApiResponse<List<SectorIndexDaily>> query(
            @RequestParam String sectorType,
            @RequestParam String sectorCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LambdaQueryWrapper<SectorIndexDaily> wrapper = new LambdaQueryWrapper<SectorIndexDaily>()
                .eq(SectorIndexDaily::getSectorType, sectorType)
                .eq(SectorIndexDaily::getSectorCode, sectorCode)
                .orderByAsc(SectorIndexDaily::getTradeDate);
        if (startDate != null) {
            wrapper.ge(SectorIndexDaily::getTradeDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(SectorIndexDaily::getTradeDate, endDate);
        }
        List<SectorIndexDaily> records = sectorIndexDailyMapper.selectList(wrapper);
        return ApiResponse.ok(records);
    }
}
