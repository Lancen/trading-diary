package com.tradingdiary.service;

import java.time.LocalDate;
import java.util.Map;

public interface MarketDataService {

    Map<String, Object> listConcepts(String keyword, LocalDate tradeDate,
                                     String sortBy, String sortDir, int page, int size);

    Map<String, Object> listIndustries(String keyword, LocalDate tradeDate,
                                       String sortBy, String sortDir, int page, int size);
}
