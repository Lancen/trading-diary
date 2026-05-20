package com.tradingdiary.service;

import com.tradingdiary.collection.model.StockDetailVO;
import com.tradingdiary.collection.model.StockListVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StockDataService {

    Map<String, Object> listStocks(String keyword, String industry, String concept,
                                   LocalDate tradeDate, String sortBy, String sortDir, int page, int size);

    StockDetailVO getStockDetail(String stockCode, LocalDate startDate, LocalDate endDate);
}
