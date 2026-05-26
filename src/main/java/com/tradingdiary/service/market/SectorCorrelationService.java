package com.tradingdiary.service.market;

import java.util.List;

public interface SectorCorrelationService {

    List<SectorCorrelation> compute(String sectorType, String sectorCode);
}
