package com.tradingdiary.service.market.impl;

import com.tradingdiary.mapper.SectorIndexDailyMapper;
import com.tradingdiary.model.vo.SectorRanking;
import com.tradingdiary.service.market.SectorRankingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 板块排名服务实现，委托 SectorIndexDailyMapper 完成排名查询
 */
@Service
public class SectorRankingServiceImpl implements SectorRankingService {

    private final SectorIndexDailyMapper sectorIndexDailyMapper;

    public SectorRankingServiceImpl(SectorIndexDailyMapper sectorIndexDailyMapper) {
        this.sectorIndexDailyMapper = sectorIndexDailyMapper;
    }

    @Override
    public List<SectorRanking> query(String sectorType, LocalDate tradeDate, String sortBy, String sortDir) {
        if (!VALID_TYPES.contains(sectorType)) {
            throw new IllegalArgumentException("Invalid sectorType: " + sectorType);
        }

        if (!VALID_SORT_FIELDS.contains(sortBy)) {
            sortBy = "amount";
        }

        // 默认取最新交易日
        if (tradeDate == null) {
            tradeDate = sectorIndexDailyMapper.selectMaxTradeDateByType(sectorType);
            if (tradeDate == null) return List.of();
        }

        LocalDate prevDate = sectorIndexDailyMapper.selectPrevTradeDateByType(sectorType, tradeDate);

        return sectorIndexDailyMapper.selectSectorRanking(
                sectorType.toLowerCase(), tradeDate, prevDate, sortBy, sortDir.toLowerCase());
    }
}