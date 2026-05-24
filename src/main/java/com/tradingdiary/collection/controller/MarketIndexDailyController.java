package com.tradingdiary.collection.controller;

import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.mapper.MarketIndexDailyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
@RequestMapping("/api/v1/admin/market-index-daily")
@PreAuthorize("hasRole('ADMIN')")
public class MarketIndexDailyController {

    private final MarketIndexDailyMapper marketIndexDailyMapper;

    public MarketIndexDailyController(MarketIndexDailyMapper marketIndexDailyMapper) {
        this.marketIndexDailyMapper = marketIndexDailyMapper;
    }

    @Operation(summary = "查询宽基指数日线数据")
    @GetMapping
    public ApiResponse<List<MarketIndexDaily>> query(
            @RequestParam String indexCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LambdaQueryWrapper<MarketIndexDaily> wrapper = new LambdaQueryWrapper<MarketIndexDaily>()
                .eq(MarketIndexDaily::getIndexCode, indexCode)
                .orderByAsc(MarketIndexDaily::getTradeDate);
        if (startDate != null) {
            wrapper.ge(MarketIndexDaily::getTradeDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(MarketIndexDaily::getTradeDate, endDate);
        }
        List<MarketIndexDaily> records = marketIndexDailyMapper.selectList(wrapper);
        return ApiResponse.ok(records);
    }

    @Operation(summary = "查询所有已采集宽基指数的最新行情")
    @GetMapping("/latest")
    public ApiResponse<List<MarketIndexDaily>> latest() {
        List<MarketIndexDaily> records = marketIndexDailyMapper.selectList(
                new LambdaQueryWrapper<MarketIndexDaily>()
                        .in(MarketIndexDaily::getIndexCode,
                                List.of("sh000001", "sz399001", "sz399006", "sh000300", "sh000905",
                                        "sh000016", "sh000688", "sh000852"))
                        .orderByDesc(MarketIndexDaily::getTradeDate)
        );

        return ApiResponse.ok(records);
    }
}
