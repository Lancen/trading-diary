package com.tradingdiary.collection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.mapper.MarketIndexDailyMapper;
import com.tradingdiary.mapper.SectorIndexDailyMapper;
import com.tradingdiary.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/sector-index-daily")
@PreAuthorize("hasRole('ADMIN')")
public class SectorIndexDailyController {

    private static final Set<String> VALID_SECTOR_TYPES = Set.of("INDUSTRY", "CONCEPT");

    private final SectorIndexDailyMapper sectorIndexDailyMapper;
    private final MarketIndexDailyMapper marketIndexDailyMapper;

    public SectorIndexDailyController(SectorIndexDailyMapper sectorIndexDailyMapper,
                                       MarketIndexDailyMapper marketIndexDailyMapper) {
        this.sectorIndexDailyMapper = sectorIndexDailyMapper;
        this.marketIndexDailyMapper = marketIndexDailyMapper;
    }

    @Operation(summary = "查询板块指数日线数据")
    @GetMapping
    public ApiResponse<List<SectorIndexDaily>> query(
            @RequestParam String sectorType,
            @RequestParam String sectorCode,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        if (!VALID_SECTOR_TYPES.contains(sectorType)) {
            return ApiResponse.fail(400, "Invalid sectorType: " + sectorType + ", must be INDUSTRY or CONCEPT");
        }
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

        if (!records.isEmpty()) {
            LocalDate minDate = records.get(0).getTradeDate();
            LocalDate maxDate = records.get(records.size() - 1).getTradeDate();
            LambdaQueryWrapper<MarketIndexDaily> mktWrapper = new LambdaQueryWrapper<MarketIndexDaily>()
                    .ge(MarketIndexDaily::getTradeDate, minDate)
                    .le(MarketIndexDaily::getTradeDate, maxDate)
                    .eq(MarketIndexDaily::getIsDeleted, false);
            List<MarketIndexDaily> mktData = marketIndexDailyMapper.selectList(mktWrapper);
            Map<LocalDate, BigDecimal> mktAmountMap = mktData.stream()
                    .filter(m -> m.getAmount() != null)
                    .collect(Collectors.toMap(MarketIndexDaily::getTradeDate, MarketIndexDaily::getAmount, (a, b) -> a));

            for (SectorIndexDaily record : records) {
                BigDecimal mktAmount = mktAmountMap.get(record.getTradeDate());
                if (mktAmount != null && mktAmount.compareTo(BigDecimal.ZERO) > 0 && record.getAmount() != null) {
                    record.setVolumePct(record.getAmount().divide(mktAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                }
            }
        }

        return ApiResponse.ok(records);
    }
}
