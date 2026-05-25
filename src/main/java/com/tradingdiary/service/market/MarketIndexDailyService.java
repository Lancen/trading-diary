package com.tradingdiary.service.market;

import com.tradingdiary.entity.MarketIndexDaily;

import java.time.LocalDate;
import java.util.List;

public interface MarketIndexDailyService {

    List<MarketIndexDaily> query(String indexCode, LocalDate startDate, LocalDate endDate);

    List<MarketIndexDaily> latest();
}
