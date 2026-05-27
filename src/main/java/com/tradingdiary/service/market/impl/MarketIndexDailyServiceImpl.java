package com.tradingdiary.service.market.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.entity.MarketIndexDaily;
import com.tradingdiary.mapper.MarketIndexDailyMapper;
import com.tradingdiary.service.market.MarketIndexDailyService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 宽基指数日线查询服务实现，提供指数行情查询
 */
@Service
public class MarketIndexDailyServiceImpl implements MarketIndexDailyService {

    private static final List<String> TRACKED_INDICES = List.of(
            "sh000001", "sz399001", "sz399006", "sh000300", "sh000905",
            "sh000016", "sh000688", "sh000852"
    );

    private final MarketIndexDailyMapper marketIndexDailyMapper;

    public MarketIndexDailyServiceImpl(MarketIndexDailyMapper marketIndexDailyMapper) {
        this.marketIndexDailyMapper = marketIndexDailyMapper;
    }

    @Override
    public List<MarketIndexDaily> query(String indexCode, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<MarketIndexDaily> wrapper = new LambdaQueryWrapper<MarketIndexDaily>()
                .eq(MarketIndexDaily::getIndexCode, indexCode)
                .orderByAsc(MarketIndexDaily::getTradeDate);
        if (startDate != null) {
            wrapper.ge(MarketIndexDaily::getTradeDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(MarketIndexDaily::getTradeDate, endDate);
        }
        return marketIndexDailyMapper.selectList(wrapper);
    }

    @Override
    public List<MarketIndexDaily> latest() {
        List<MarketIndexDaily> result = new ArrayList<>();
        for (String indexCode : TRACKED_INDICES) {
            MarketIndexDaily latestRecord = marketIndexDailyMapper.selectOne(
                    new LambdaQueryWrapper<MarketIndexDaily>()
                            .eq(MarketIndexDaily::getIndexCode, indexCode)
                            .orderByDesc(MarketIndexDaily::getTradeDate)
                            .last("LIMIT 1")
            );
            if (latestRecord != null) {
                result.add(latestRecord);
            }
        }
        return result;
    }
}
