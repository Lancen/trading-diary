package com.tradingdiary.service.collection;

import java.time.LocalDate;

public interface StockInfoCleanseService {

    int cleanse(String rawJson, LocalDate snapshotDate);
}
