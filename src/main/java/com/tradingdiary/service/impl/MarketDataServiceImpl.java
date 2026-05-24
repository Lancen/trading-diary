package com.tradingdiary.service.impl;

import com.tradingdiary.collection.model.ConceptIndustryVO;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.service.MarketDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataServiceImpl implements MarketDataService {

    private static final List<String> ALLOWED_SORT_COLUMNS = List.of(
            "stockCount", "marginBalance", "marginChange", "shortBalance", "shortChange");

    private final ConceptMapper conceptMapper;
    private final IndustryMapper industryMapper;

    public MarketDataServiceImpl(ConceptMapper conceptMapper, IndustryMapper industryMapper) {
        this.conceptMapper = conceptMapper;
        this.industryMapper = industryMapper;
    }

    @Override
    public Map<String, Object> listConcepts(String keyword, LocalDate tradeDate,
                                            String sortBy, String sortDir, int page, int size) {
        String sortColumn = ALLOWED_SORT_COLUMNS.contains(sortBy) ? sortBy : "marginBalance";
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int offset = (page - 1) * size;
        String dateStr = tradeDate != null ? tradeDate.toString() : null;

        List<ConceptIndustryVO> records = conceptMapper.selectConceptList(
                keyword, dateStr, sortColumn, direction, offset, size);
        long total = conceptMapper.countConceptList(keyword, dateStr);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> listIndustries(String keyword, LocalDate tradeDate,
                                              String sortBy, String sortDir, int page, int size) {
        String sortColumn = ALLOWED_SORT_COLUMNS.contains(sortBy) ? sortBy : "marginBalance";
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int offset = (page - 1) * size;
        String dateStr = tradeDate != null ? tradeDate.toString() : null;

        List<ConceptIndustryVO> records = industryMapper.selectIndustryList(
                keyword, dateStr, sortColumn, direction, offset, size);
        long total = industryMapper.countIndustryList(keyword, dateStr);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }
}
