package com.tradingdiary.service.market;

import java.time.LocalDate;
import java.util.List;

public interface MacroMarginService {

    List<MacroMarginDaily> aggregate(LocalDate startDate, LocalDate endDate);
}
