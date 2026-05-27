package com.tradingdiary.service.market.impl;

import com.tradingdiary.mapper.SectorMarginMapper;
import com.tradingdiary.service.market.SectorMarginDaily;
import com.tradingdiary.service.market.SectorMarginService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 板块两融占比服务实现，委托 SectorMarginMapper 完成聚合查询
 */
@Service
public class SectorMarginServiceImpl implements SectorMarginService {

    private static final Set<String> VALID_SECTOR_TYPES = Set.of("INDUSTRY", "CONCEPT");

    private final SectorMarginMapper sectorMarginMapper;

    public SectorMarginServiceImpl(SectorMarginMapper sectorMarginMapper) {
        this.sectorMarginMapper = sectorMarginMapper;
    }

    @Override
    public List<SectorMarginDaily> aggregate(String sectorType, String sectorCode, LocalDate startDate, LocalDate endDate) {
        if (!VALID_SECTOR_TYPES.contains(sectorType)) {
            throw new IllegalArgumentException("Invalid sectorType: " + sectorType);
        }
        return sectorMarginMapper.selectSectorMarginAggregate(sectorType, sectorCode, startDate, endDate);
    }
}