package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.entity.StockBalanceSheet;
import com.tradingdiary.entity.StockCashFlow;
import com.tradingdiary.entity.StockFinancialIndicator;
import com.tradingdiary.entity.StockIncome;
import com.tradingdiary.mapper.StockBalanceSheetMapper;
import com.tradingdiary.mapper.StockCashFlowMapper;
import com.tradingdiary.mapper.StockFinancialIndicatorMapper;
import com.tradingdiary.mapper.StockIncomeMapper;
import com.tradingdiary.service.collection.FinancialStatementCleanseService;
import com.tradingdiary.util.BatchSqlRunner;
import com.tradingdiary.util.JsonNodeHelper;
import com.tradingdiary.util.UpsertHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.tradingdiary.util.JsonNodeHelper.safeDecimal;
import static com.tradingdiary.util.JsonNodeHelper.safeText;
import static com.tradingdiary.util.UpsertHelper.partition;

@Service
public class FinancialStatementCleanseServiceImpl implements FinancialStatementCleanseService {

    private static final Logger log = LoggerFactory.getLogger(FinancialStatementCleanseServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockIncomeMapper incomeMapper;
    private final StockBalanceSheetMapper balanceSheetMapper;
    private final StockCashFlowMapper cashFlowMapper;
    private final StockFinancialIndicatorMapper indicatorMapper;
    private final BatchSqlRunner batchSqlRunner;
    private final ObjectMapper objectMapper;

    public FinancialStatementCleanseServiceImpl(StockIncomeMapper incomeMapper,
                                                 StockBalanceSheetMapper balanceSheetMapper,
                                                 StockCashFlowMapper cashFlowMapper,
                                                 StockFinancialIndicatorMapper indicatorMapper,
                                                 BatchSqlRunner batchSqlRunner,
                                                 ObjectMapper objectMapper) {
        this.incomeMapper = incomeMapper;
        this.balanceSheetMapper = balanceSheetMapper;
        this.cashFlowMapper = cashFlowMapper;
        this.indicatorMapper = indicatorMapper;
        this.batchSqlRunner = batchSqlRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int cleanseIncome(String rawJson) {
        List<StockIncome> entities = parseIncome(rawJson);
        if (entities.isEmpty()) return 0;
        return upsertByCodeAndReport(entities,
                incomeMapper, StockIncome::getStockCode, StockIncome::getReportDate);
    }

    @Override
    @Transactional
    public int cleanseBalanceSheet(String rawJson) {
        List<StockBalanceSheet> entities = parseBalanceSheet(rawJson);
        if (entities.isEmpty()) return 0;
        return upsertByCodeAndReport(entities,
                balanceSheetMapper, StockBalanceSheet::getStockCode, StockBalanceSheet::getReportDate);
    }

    @Override
    @Transactional
    public int cleanseCashFlow(String rawJson) {
        List<StockCashFlow> entities = parseCashFlow(rawJson);
        if (entities.isEmpty()) return 0;
        return upsertByCodeAndReport(entities,
                cashFlowMapper, StockCashFlow::getStockCode, StockCashFlow::getReportDate);
    }

    @Override
    @Transactional
    public int cleanseFinancialIndicator(String rawJson) {
        List<StockFinancialIndicator> entities = parseFinancialIndicator(rawJson);
        if (entities.isEmpty()) return 0;
        return upsertByCodeAndReport(entities,
                indicatorMapper, StockFinancialIndicator::getStockCode, StockFinancialIndicator::getReportDate);
    }

    @Override
    public List<StockIncome> parseIncome(String rawJson) {
        List<StockIncome> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;
            for (JsonNode node : root) {
                StockIncome entity = new StockIncome();
                String rawCode = safeText(node, "股票代码");
                entity.setStockCode(stripMarketPrefix(rawCode));
                entity.setReportDate(parseReportDate(safeText(node, "报告期")));
                entity.setReportType(inferReportType(entity.getReportDate()));
                entity.setOperatingRevenue(safeDecimal(node, "营业总收入"));
                entity.setTotalRevenue(safeDecimal(node, "营业总收入"));
                entity.setOperatingCost(safeDecimal(node, "营业成本"));
                entity.setTotalCost(safeDecimal(node, "营业总成本"));
                entity.setRdExpense(safeDecimal(node, "研发费用"));
                entity.setSellingExpense(safeDecimal(node, "销售费用"));
                entity.setAdminExpense(safeDecimal(node, "管理费用"));
                entity.setFinanceExpense(safeDecimal(node, "财务费用"));
                entity.setOperatingProfit(safeDecimal(node, "营业利润"));
                entity.setTotalProfit(safeDecimal(node, "利润总额"));
                entity.setNetProfit(safeDecimal(node, "净利润"));
                entity.setNpParent(safeDecimal(node, "归母净利润"));
                entity.setNpMinority(safeDecimal(node, "少数股东损益"));
                entity.setDeductedNp(safeDecimal(node, "扣非净利润"));
                entity.setBasicEps(safeDecimal(node, "每股收益"));
                if (entity.getStockCode() != null && entity.getReportDate() != null) {
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse income statement JSON", e);
            throw new RuntimeException("解析利润表数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<StockBalanceSheet> parseBalanceSheet(String rawJson) {
        List<StockBalanceSheet> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;
            for (JsonNode node : root) {
                StockBalanceSheet entity = new StockBalanceSheet();
                String rawCode = safeText(node, "股票代码");
                entity.setStockCode(stripMarketPrefix(rawCode));
                entity.setReportDate(parseReportDate(safeText(node, "报告期")));
                entity.setReportType(inferReportType(entity.getReportDate()));
                entity.setTotalAssets(safeDecimal(node, "总资产"));
                entity.setCurrentAssets(safeDecimal(node, "流动资产"));
                entity.setCash(safeDecimal(node, "货币资金"));
                entity.setAccountsReceivable(safeDecimal(node, "应收账款"));
                entity.setInventory(safeDecimal(node, "存货"));
                entity.setFixedAssets(safeDecimal(node, "固定资产"));
                entity.setGoodwill(safeDecimal(node, "商誉"));
                entity.setTotalLiabilities(safeDecimal(node, "总负债"));
                entity.setCurrentLiabilities(safeDecimal(node, "流动负债"));
                entity.setInterestBearingDebt(safeDecimal(node, "有息负债"));
                entity.setTotalEquity(safeDecimal(node, "所有者权益"));
                entity.setEquityParent(safeDecimal(node, "归母权益"));
                entity.setMinorityInterest(safeDecimal(node, "少数股东权益"));
                if (entity.getStockCode() != null && entity.getReportDate() != null) {
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse balance sheet JSON", e);
            throw new RuntimeException("解析资产负债表数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<StockCashFlow> parseCashFlow(String rawJson) {
        List<StockCashFlow> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;
            for (JsonNode node : root) {
                StockCashFlow entity = new StockCashFlow();
                String rawCode = safeText(node, "股票代码");
                entity.setStockCode(stripMarketPrefix(rawCode));
                entity.setReportDate(parseReportDate(safeText(node, "报告期")));
                entity.setReportType(inferReportType(entity.getReportDate()));
                entity.setOperatingCashFlow(safeDecimal(node, "经营活动现金流净额"));
                entity.setInvestingCashFlow(safeDecimal(node, "投资活动现金流净额"));
                entity.setFinancingCashFlow(safeDecimal(node, "筹资活动现金流净额"));
                entity.setCapex(safeDecimal(node, "资本开支"));
                entity.setDepreciation(safeDecimal(node, "折旧与摊销"));
                entity.setCashDividendPaid(safeDecimal(node, "分配股利支付的现金"));
                BigDecimal ocf = entity.getOperatingCashFlow();
                BigDecimal capex = entity.getCapex();
                if (ocf != null && capex != null) {
                    entity.setFreeCashFlow(ocf.subtract(capex));
                }
                if (entity.getStockCode() != null && entity.getReportDate() != null) {
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse cash flow JSON", e);
            throw new RuntimeException("解析现金流量表数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<StockFinancialIndicator> parseFinancialIndicator(String rawJson) {
        List<StockFinancialIndicator> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) return result;
            for (JsonNode node : root) {
                StockFinancialIndicator entity = new StockFinancialIndicator();
                String rawCode = safeText(node, "股票代码");
                entity.setStockCode(stripMarketPrefix(rawCode));
                entity.setReportDate(parseReportDate(safeText(node, "报告期")));
                entity.setReportType(inferReportType(entity.getReportDate()));
                entity.setGrossMargin(safeDecimal(node, "毛利率"));
                entity.setNetMargin(safeDecimal(node, "净利率"));
                entity.setRoe(safeDecimal(node, "净资产收益率"));
                entity.setRoa(safeDecimal(node, "总资产收益率"));
                entity.setDebtRatio(safeDecimal(node, "资产负债率"));
                entity.setCurrentRatio(safeDecimal(node, "流动比率"));
                entity.setQuickRatio(safeDecimal(node, "速动比率"));
                entity.setArTurnoverDays(safeDecimal(node, "应收账款周转天数"));
                entity.setInventoryTurnoverDays(safeDecimal(node, "存货周转天数"));
                entity.setOcfToNp(safeDecimal(node, "经营现金流/净利润"));
                entity.setRevenueYoy(safeDecimal(node, "营收同比增长率"));
                entity.setNpYoy(safeDecimal(node, "净利润同比增长率"));
                if (entity.getStockCode() != null && entity.getReportDate() != null) {
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse financial indicator JSON", e);
            throw new RuntimeException("解析财务指标数据失败: " + e.getMessage(), e);
        }
        return result;
    }

    private <T> int upsertByCodeAndReport(List<T> entities,
                                           com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                                           java.util.function.Function<T, String> codeFn,
                                           java.util.function.Function<T, LocalDate> dateFn) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) entities.get(0).getClass();
        List<T> existing = mapper.selectList(new LambdaQueryWrapper<>(clazz));
        java.util.function.Function<T, String> compositeKey = e -> codeFn.apply(e) + "_" + dateFn.apply(e);
        UpsertHelper.PartitionResult<T> result = partition(entities, existing,
                compositeKey, compositeKey,
                (src, tgt) -> {
                    try {
                        var idField = src.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        var idField2 = tgt.getClass().getDeclaredField("id");
                        idField2.setAccessible(true);
                        idField2.set(tgt, idField.get(src));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
        int count = 0;
        if (result.hasInserts()) count += batchSqlRunner.batchInsert(result.getToInsert());
        if (result.hasUpdates()) count += batchSqlRunner.batchUpdate(result.getToUpdate());
        log.info("Financial statement upsert: {} records for {}", count, clazz.getSimpleName());
        return count;
    }

    private LocalDate parseReportDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            String cleaned = dateStr.replaceAll("[^0-9]", "");
            if (cleaned.length() >= 8) {
                return LocalDate.parse(cleaned.substring(0, 8), DATE_FMT);
            }
            return null;
        } catch (Exception e) {
            log.debug("Failed to parse report date: {}", dateStr);
            return null;
        }
    }

    private String inferReportType(LocalDate reportDate) {
        if (reportDate == null) return null;
        int month = reportDate.getMonthValue();
        return switch (month) {
            case 3 -> "Q1";
            case 6 -> "H1";
            case 9 -> "Q3";
            case 12 -> "Annual";
            default -> "Other";
        };
    }

    private String stripMarketPrefix(String code) {
        return JsonNodeHelper.stripMarketPrefix(code);
    }
}
