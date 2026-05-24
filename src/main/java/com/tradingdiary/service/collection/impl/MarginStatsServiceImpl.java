package com.tradingdiary.service.collection.impl;

import com.tradingdiary.collection.model.MarginSummaryVO;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.service.collection.MarginStatsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MarginStatsServiceImpl implements MarginStatsService {

    private final MarginDailyMapper marginDailyMapper;

    public MarginStatsServiceImpl(MarginDailyMapper marginDailyMapper) {
        this.marginDailyMapper = marginDailyMapper;
    }

    @Override
    public MarginSummaryVO getMarginSummary(LocalDate tradeDate) {
        String sqlDate = tradeDate != null ? tradeDate.toString() : null;
        MarginSummaryVO vo = new MarginSummaryVO();
        vo.setTotalMarginBalance(marginDailyMapper.sumMarginBalance(sqlDate));
        vo.setTotalShortBalance(marginDailyMapper.sumShortBalance(sqlDate));
        vo.setTotalBalance(marginDailyMapper.sumTotalBalance(sqlDate));
        vo.setStockCount(marginDailyMapper.countDistinctStocks(sqlDate));
        vo.setTradeDate(tradeDate);
        return vo;
    }
}
