package com.tradingdiary.service.collection;

import java.util.List;
import java.util.Map;

public interface ConstituentImportService {

    List<Map<String, Object>> listFiles();

    Map<String, Object> importFromFile(String filename);
}
