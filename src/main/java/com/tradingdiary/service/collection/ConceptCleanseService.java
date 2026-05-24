package com.tradingdiary.service.collection;

import java.time.LocalDate;

public interface ConceptCleanseService {

    int cleanseNames(String rawJson);

    int cleanseCons(String rawJson, String conceptCode, LocalDate snapDate);
}
