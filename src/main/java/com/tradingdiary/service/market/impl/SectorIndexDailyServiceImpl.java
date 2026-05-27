package com.tradingdiary.service.market.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.mapper.MarketIndexDailyMapper;
import com.tradingdiary.mapper.SectorIndexDailyMapper;
import com.tradingdiary.service.market.SectorIndexDailyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 板块指数日线查询服务实现，查询板块指数数据并计算成交占比
 */
@Service
public class SectorIndexDailyServiceImpl implements SectorIndexDailyService {

    private final SectorIndexDailyMapper sectorIndexDailyMapper;
    private final MarketIndexDailyMapper marketIndexDailyMapper;

    public SectorIndexDailyServiceImpl(SectorIndexDailyMapper sectorIndexDailyMapper,
                                        MarketIndexDailyMapper marketIndexDailyMapper) {
        this.sectorIndexDailyMapper = sectorIndexDailyMapper;
        this.marketIndexDailyMapper = marketIndexDailyMapper;
    }

    @Override
    public List<SectorIndexDaily> query(String sectorType, String sectorCode,
                                          LocalDate startDate, LocalDate endDate) {
        if (!VALID_SECTOR_TYPES.contains(sectorType)) {
            throw new IllegalArgumentException("Invalid sectorType: " + sectorType);
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
            computeVolumePct(records);
        }

        return records;
    }

    /** 计算每条记录的成交占比（板块成交额 / 全市场成交额 × 100） */
    private void computeVolumePct(List<SectorIndexDaily> records) {
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
}