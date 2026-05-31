package com.tradingdiary.collection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.entity.StockBalanceSheet;
import com.tradingdiary.entity.StockCashFlow;
import com.tradingdiary.entity.StockDailyValuation;
import com.tradingdiary.entity.StockFinancialIndicator;
import com.tradingdiary.entity.StockIncome;
import com.tradingdiary.mapper.StockBalanceSheetMapper;
import com.tradingdiary.mapper.StockCashFlowMapper;
import com.tradingdiary.mapper.StockDailyValuationMapper;
import com.tradingdiary.mapper.StockFinancialIndicatorMapper;
import com.tradingdiary.mapper.StockIncomeMapper;
import com.tradingdiary.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/financial")
public class FinancialDataController {

    private final StockIncomeMapper incomeMapper;
    private final StockBalanceSheetMapper balanceSheetMapper;
    private final StockCashFlowMapper cashFlowMapper;
    private final StockFinancialIndicatorMapper indicatorMapper;
    private final StockDailyValuationMapper valuationMapper;

    public FinancialDataController(StockIncomeMapper incomeMapper,
                                    StockBalanceSheetMapper balanceSheetMapper,
                                    StockCashFlowMapper cashFlowMapper,
                                    StockFinancialIndicatorMapper indicatorMapper,
                                    StockDailyValuationMapper valuationMapper) {
        this.incomeMapper = incomeMapper;
        this.balanceSheetMapper = balanceSheetMapper;
        this.cashFlowMapper = cashFlowMapper;
        this.indicatorMapper = indicatorMapper;
        this.valuationMapper = valuationMapper;
    }

    @Operation(summary = "获取利润表数据")
    @GetMapping("/income/{stockCode}")
    public ApiResponse<List<StockIncome>> getIncome(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "4") int limit) {
        List<StockIncome> list = incomeMapper.selectList(
                new LambdaQueryWrapper<StockIncome>()
                        .eq(StockIncome::getStockCode, stockCode)
                        .orderByDesc(StockIncome::getReportDate)
                        .last("LIMIT " + limit));
        return ApiResponse.ok(list);
    }

    @Operation(summary = "获取资产负债表数据")
    @GetMapping("/balance-sheet/{stockCode}")
    public ApiResponse<List<StockBalanceSheet>> getBalanceSheet(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "4") int limit) {
        List<StockBalanceSheet> list = balanceSheetMapper.selectList(
                new LambdaQueryWrapper<StockBalanceSheet>()
                        .eq(StockBalanceSheet::getStockCode, stockCode)
                        .orderByDesc(StockBalanceSheet::getReportDate)
                        .last("LIMIT " + limit));
        return ApiResponse.ok(list);
    }

    @Operation(summary = "获取现金流量表数据")
    @GetMapping("/cash-flow/{stockCode}")
    public ApiResponse<List<StockCashFlow>> getCashFlow(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "4") int limit) {
        List<StockCashFlow> list = cashFlowMapper.selectList(
                new LambdaQueryWrapper<StockCashFlow>()
                        .eq(StockCashFlow::getStockCode, stockCode)
                        .orderByDesc(StockCashFlow::getReportDate)
                        .last("LIMIT " + limit));
        return ApiResponse.ok(list);
    }

    @Operation(summary = "获取财务指标数据")
    @GetMapping("/indicator/{stockCode}")
    public ApiResponse<List<StockFinancialIndicator>> getIndicator(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "4") int limit) {
        List<StockFinancialIndicator> list = indicatorMapper.selectList(
                new LambdaQueryWrapper<StockFinancialIndicator>()
                        .eq(StockFinancialIndicator::getStockCode, stockCode)
                        .orderByDesc(StockFinancialIndicator::getReportDate)
                        .last("LIMIT " + limit));
        return ApiResponse.ok(list);
    }

    @Operation(summary = "获取每日估值数据")
    @GetMapping("/valuation/{stockCode}")
    public ApiResponse<List<StockDailyValuation>> getValuation(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "30") int limit) {
        List<StockDailyValuation> list = valuationMapper.selectList(
                new LambdaQueryWrapper<StockDailyValuation>()
                        .eq(StockDailyValuation::getStockCode, stockCode)
                        .orderByDesc(StockDailyValuation::getTradeDate)
                        .last("LIMIT " + limit));
        return ApiResponse.ok(list);
    }
}
