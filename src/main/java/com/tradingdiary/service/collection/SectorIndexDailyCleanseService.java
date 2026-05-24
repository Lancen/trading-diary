package com.tradingdiary.service.collection;

public interface SectorIndexDailyCleanseService {

    int cleanse(String rawJson, String sectorType, String sectorCode);
}
