package com.tradingdiary.service.market;

/**
 * 板块成分股条目 VO
 */
public record SectorStockItem(
        String stockCode,  // 股票代码
        String stockName   // 股票名称
) {
}
