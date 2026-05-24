package com.tradingdiary.service.collection;

import java.time.LocalDate;

public interface MarginCleanseService {

    int cleanse(String rawJson, String exchange, LocalDate tradeDate);
}
