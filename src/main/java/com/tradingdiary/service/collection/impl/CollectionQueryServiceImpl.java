package com.tradingdiary.service.collection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.DataCollectionLogMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.MarginMacroMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import com.tradingdiary.service.collection.CollectionQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectionQueryServiceImpl implements CollectionQueryService {

    private static final Map<String, String> DATA_TYPE_LABELS = new LinkedHashMap<>();

    static {
        DATA_TYPE_LABELS.put("STOCK_INFO", "股票行情（含日线）");
        DATA_TYPE_LABELS.put("TRADE_CALENDAR", "交易日历");
        DATA_TYPE_LABELS.put("INDUSTRY_NAME", "行业板块分类");
        DATA_TYPE_LABELS.put("CONCEPT_NAME", "概念板块分类");
        DATA_TYPE_LABELS.put("MARGIN_DAILY_SSE", "两融明细(沪市)");
        DATA_TYPE_LABELS.put("MARGIN_DAILY_SZSE", "两融明细(深市)");
        DATA_TYPE_LABELS.put("MARGIN_MACRO_SSE", "两融总量(沪市)");
        DATA_TYPE_LABELS.put("MARGIN_MACRO_SZSE", "两融总量(深市)");
    }

    private final DataCollectionLogMapper logMapper;
    private final TradeCalendarMapper tradeCalendarMapper;
    private final StockInfoMapper stockInfoMapper;
    private final IndustryMapper industryMapper;
    private final ConceptMapper conceptMapper;
    private final MarginDailyMapper marginDailyMapper;
    private final MarginMacroMapper marginMacroMapper;

    public CollectionQueryServiceImpl(DataCollectionLogMapper logMapper,
                                       TradeCalendarMapper tradeCalendarMapper,
                                       StockInfoMapper stockInfoMapper,
                                       IndustryMapper industryMapper,
                                       ConceptMapper conceptMapper,
                                       MarginDailyMapper marginDailyMapper,
                                       MarginMacroMapper marginMacroMapper) {
        this.logMapper = logMapper;
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.stockInfoMapper = stockInfoMapper;
        this.industryMapper = industryMapper;
        this.conceptMapper = conceptMapper;
        this.marginDailyMapper = marginDailyMapper;
        this.marginMacroMapper = marginMacroMapper;
    }

    @Override
    public List<CollectionStatusVO> getCollectionStatus() {
        List<CollectionStatusVO> statusList = new ArrayList<>();

        for (Map.Entry<String, String> entry : DATA_TYPE_LABELS.entrySet()) {
            String dataType = entry.getKey();
            String label = entry.getValue();

            DataCollectionLog fetchLog = logMapper.selectLatestByDataTypeAndJobType(dataType, "FETCH");
            DataCollectionLog cleanseLog = logMapper.selectLatestByDataTypeAndJobType(dataType, "CLEANSE");

            CollectionStatusVO vo = new CollectionStatusVO();
            vo.setDataType(dataType);
            vo.setDataTypeLabel(label);
            vo.setLastFetch(buildJobStatus(fetchLog));
            vo.setLastCleanse(buildJobStatus(cleanseLog));
            vo.setLastDataDate(queryLastDataDate(dataType));
            statusList.add(vo);
        }

        return statusList;
    }

    @Override
    public List<DataCollectionLog> getRecentLogs(String dataType, int limit) {
        return logMapper.selectRecentByDataType(dataType, limit);
    }

    @Override
    public LocalDate getLatestTradeDate() {
        TradeCalendar cal = tradeCalendarMapper.selectOne(
                new LambdaQueryWrapper<TradeCalendar>()
                        .eq(TradeCalendar::getIsTradingDay, 1)
                        .le(TradeCalendar::getTradeDate, LocalDate.now())
                        .orderByDesc(TradeCalendar::getTradeDate)
                        .last("LIMIT 1")
        );
        return cal != null ? cal.getTradeDate() : LocalDate.now();
    }

    @Override
    public boolean isValidDataType(String dataType) {
        return DATA_TYPE_LABELS.containsKey(dataType);
    }

    private LocalDateTime queryLastDataDate(String dataType) {
        switch (dataType) {
            case "STOCK_INFO": {
                LocalDate d = stockInfoMapper.selectMaxTradeDate();
                return d != null ? LocalDateTime.of(d, LocalTime.MIN) : null;
            }
            case "TRADE_CALENDAR": {
                LocalDate d = tradeCalendarMapper.selectMaxCalDate();
                return d != null ? LocalDateTime.of(d, LocalTime.MIN) : null;
            }
            case "INDUSTRY_NAME":
                return industryMapper.selectMaxCreatedAt();
            case "CONCEPT_NAME":
                return conceptMapper.selectMaxCreatedAt();
            case "MARGIN_DAILY_SSE": {
                LocalDate d = marginDailyMapper.selectMaxTradeDate("SSE");
                return d != null ? LocalDateTime.of(d, LocalTime.MIN) : null;
            }
            case "MARGIN_DAILY_SZSE": {
                LocalDate d = marginDailyMapper.selectMaxTradeDate("SZSE");
                return d != null ? LocalDateTime.of(d, LocalTime.MIN) : null;
            }
            case "MARGIN_MACRO_SSE": {
                LocalDate d = marginMacroMapper.selectMaxTradeDate("SSE");
                return d != null ? LocalDateTime.of(d, LocalTime.MIN) : null;
            }
            case "MARGIN_MACRO_SZSE": {
                LocalDate d = marginMacroMapper.selectMaxTradeDate("SZSE");
                return d != null ? LocalDateTime.of(d, LocalTime.MIN) : null;
            }
            default:
                return null;
        }
    }

    private static CollectionStatusVO.JobStatus buildJobStatus(DataCollectionLog log) {
        if (log == null) {
            return null;
        }
        CollectionStatusVO.JobStatus js = new CollectionStatusVO.JobStatus();
        js.setStatus(log.getStatus());
        js.setStartedAt(log.getStartedAt());
        js.setCompletedAt(log.getCompletedAt());
        js.setRecordCount(log.getRecordCount());
        js.setErrorMsg(log.getErrorMsg());
        return js;
    }
}
