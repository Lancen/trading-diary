package com.tradingdiary.collection.controller;

import com.tradingdiary.collection.model.MarginSummaryVO;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/margin-stats")
@PreAuthorize("hasRole('ADMIN')")
public class MarginStatsController {

    private final MarginDailyMapper marginDailyMapper;

    public MarginStatsController(MarginDailyMapper marginDailyMapper) {
        this.marginDailyMapper = marginDailyMapper;
    }

    @Operation(summary = "获取融资统计总量")
    @GetMapping("/summary")
    public ApiResponse<MarginSummaryVO> summary(@RequestParam(required = false) LocalDate tradeDate) {
        String sqlDate = tradeDate != null ? tradeDate.toString() : null;
        MarginSummaryVO vo = new MarginSummaryVO();
        vo.setTotalMarginBalance(marginDailyMapper.sumMarginBalance(sqlDate));
        vo.setTotalShortBalance(marginDailyMapper.sumShortBalance(sqlDate));
        vo.setTotalBalance(marginDailyMapper.sumTotalBalance(sqlDate));
        vo.setStockCount(marginDailyMapper.countDistinctStocks(sqlDate));
        vo.setTradeDate(tradeDate);
        return ApiResponse.ok(vo);
    }
}
