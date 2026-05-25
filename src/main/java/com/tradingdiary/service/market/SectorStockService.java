package com.tradingdiary.service.market;

import java.util.List;

public interface SectorStockService {

    List<SectorStockItem> listIndustryStocks(String industryCode);

    List<SectorStockItem> listConceptStocks(String conceptCode);
}
