package com.tradingdiary.service.market.impl;

import com.tradingdiary.mapper.CrowdednessMapper;
import com.tradingdiary.service.market.CrowdednessService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 拥挤度服务实现，计算每日市场拥挤度指标
 */
@Service
public class CrowdednessServiceImpl implements CrowdednessService {

    private static final BigDecimal DEFAULT_END_OFFSET_YEARS = BigDecimal.valueOf(3);

    private final CrowdednessMapper crowdednessMapper;

    public CrowdednessServiceImpl(CrowdednessMapper crowdednessMapper) {
        this.crowdednessMapper = crowdednessMapper;
    }

    @Override
    public List<CrowdednessDaily> query(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            endDate = crowdednessMapper.selectLatestTradeDate();
            if (endDate == null) {
                return List.of();
            }
        }
        if (startDate == null) {
            startDate = endDate.minusYears(DEFAULT_END_OFFSET_YEARS.longValue());
        }

        List<Map<String, Object>> rows = crowdednessMapper.selectCrowdednessDaily(startDate, endDate);

        return rows.stream().map(row -> {
            LocalDate tradeDate = toLocalDate(row.get("trade_date"));
            BigDecimal totalAmount = toBigDecimal(row.get("total_amount"));
            BigDecimal topAmount = toBigDecimal(row.get("top_amount"));
            BigDecimal crowdedness = toBigDecimal(row.get("crowdedness"));
            int totalStocks = toInt(row.get("total_stocks"));
            int topStocks = toInt(row.get("top_stocks"));
            return new CrowdednessDaily(tradeDate, crowdedness, totalAmount, topAmount, totalStocks, topStocks);
        }).toList();
    }

    private LocalDate toLocalDate(Object val) {
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date sd) return sd.toLocalDate();
        return LocalDate.parse(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal bd) return bd;
        if (val == null) return BigDecimal.ZERO;
        return new BigDecimal(val.toString());
    }

    private int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }
}
