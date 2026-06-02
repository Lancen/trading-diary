package com.tradingdiary.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 拥挤度服务，提供市场拥挤度指标查询
 * <p>
 * 拥挤度 = 每日成交额排名前5%个股的总成交额 / 全市场总成交额
 * 历史表明45%是风险边界，突破45%往往对应市场大变化
 */
public interface CrowdednessService {

    /**
     * 查询指定日期范围内的拥挤度指标
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含），null时取最新
     * @return 拥挤度指标列表，按日期升序
     */
    List<CrowdednessDaily> query(LocalDate startDate, LocalDate endDate);

    /**
     * 单日拥挤度数据
     */
    record CrowdednessDaily(
            LocalDate tradeDate,
            BigDecimal crowdedness,
            BigDecimal totalAmount,
            BigDecimal topAmount,
            int totalStocks,
            int topStocks
    ) {}
}
