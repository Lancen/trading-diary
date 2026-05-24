package com.tradingdiary.service.market;

import java.time.LocalDate;
import java.util.List;

public interface SectorMarginService {

    List<SectorMarginDaily> aggregate(String sectorType, String sectorCode, LocalDate startDate, LocalDate endDate);
}
