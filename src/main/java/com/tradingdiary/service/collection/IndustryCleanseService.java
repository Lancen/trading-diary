package com.tradingdiary.service.collection;

import java.time.LocalDate;

public interface IndustryCleanseService {

    int cleanseNames(String rawJson);

    int cleanseCons(String rawJson, String industryCode, LocalDate snapDate);
}
