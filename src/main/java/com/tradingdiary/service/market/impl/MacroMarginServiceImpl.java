package com.tradingdiary.service.market.impl;

import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.service.market.MacroMarginDaily;
import com.tradingdiary.service.market.MacroMarginService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 融资融券宏观汇总服务实现，委托 MarginMacroMapper 聚合全市场两融总量数据
 */
@Service
public class MacroMarginServiceImpl implements MacroMarginService {

    private final MarginMacroMapper marginMacroMapper;

    public MacroMarginServiceImpl(MarginMacroMapper marginMacroMapper) {
        this.marginMacroMapper = marginMacroMapper;
    }

    @Override
    public List<MacroMarginDaily> aggregate(LocalDate startDate, LocalDate endDate) {
        return marginMacroMapper.selectMacroMarginDailyAggregate(startDate, endDate);
    }
}