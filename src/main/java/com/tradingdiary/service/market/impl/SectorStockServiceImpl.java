package com.tradingdiary.service.market.impl;

import com.tradingdiary.mapper.SectorStockMapper;
import com.tradingdiary.service.market.SectorStockItem;
import com.tradingdiary.service.market.SectorStockService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 板块成分股查询服务实现，委托 SectorStockMapper 查询行业/概念板块成分股
 */
@Service
public class SectorStockServiceImpl implements SectorStockService {

    private final SectorStockMapper sectorStockMapper;

    public SectorStockServiceImpl(SectorStockMapper sectorStockMapper) {
        this.sectorStockMapper = sectorStockMapper;
    }

    @Override
    public List<SectorStockItem> listIndustryStocks(String industryCode) {
        return sectorStockMapper.selectIndustryStocks(industryCode);
    }

    @Override
    public List<SectorStockItem> listConceptStocks(String conceptCode) {
        return sectorStockMapper.selectConceptStocks(conceptCode);
    }
}