package com.tradingdiary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.collection.model.StockDetailVO;
import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.entity.MarginDaily;
import com.tradingdiary.entity.StockConcept;
import com.tradingdiary.entity.StockDaily;
import com.tradingdiary.entity.StockIndustry;
import com.tradingdiary.entity.StockInfo;
import com.tradingdiary.mapper.MarginDailyMapper;
import com.tradingdiary.mapper.StockConceptMapper;
import com.tradingdiary.mapper.StockDailyMapper;
import com.tradingdiary.mapper.StockIndustryMapper;
import com.tradingdiary.mapper.StockInfoMapper;
import com.tradingdiary.service.StockDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockDataServiceImpl implements StockDataService {

    private static final List<String> ALLOWED_SORT_COLUMNS = List.of(
            "changePct", "volume", "marginBalance", "marginChange", "shortBalance", "shortChange");

    private final StockInfoMapper stockInfoMapper;
    private final StockDailyMapper stockDailyMapper;
    private final MarginDailyMapper marginDailyMapper;
    private final StockIndustryMapper stockIndustryMapper;
    private final StockConceptMapper stockConceptMapper;

    public StockDataServiceImpl(StockInfoMapper stockInfoMapper,
                                 StockDailyMapper stockDailyMapper,
                                 MarginDailyMapper marginDailyMapper,
                                 StockIndustryMapper stockIndustryMapper,
                                 StockConceptMapper stockConceptMapper) {
        this.stockInfoMapper = stockInfoMapper;
        this.stockDailyMapper = stockDailyMapper;
        this.marginDailyMapper = marginDailyMapper;
        this.stockIndustryMapper = stockIndustryMapper;
        this.stockConceptMapper = stockConceptMapper;
    }

    @Override
    public Map<String, Object> listStocks(String keyword, String industry, String concept,
                                          LocalDate tradeDate, String sortBy, String sortDir,
                                          int page, int size) {
        String sortColumn = ALLOWED_SORT_COLUMNS.contains(sortBy) ? sortBy : "changePct";
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        int offset = (page - 1) * size;
        String dateStr = tradeDate != null ? tradeDate.toString() : null;

        List<StockListVO> records = stockInfoMapper.selectStockList(
                keyword, industry, concept, dateStr, sortColumn, direction, offset, size);
        long total = stockInfoMapper.countStockList(keyword, industry, concept, dateStr);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    @Override
    public StockDetailVO getStockDetail(String stockCode, LocalDate startDate, LocalDate endDate) {
        StockDetailVO vo = new StockDetailVO();
        vo.setStockCode(stockCode);

        StockInfo info = stockInfoMapper.selectOne(
                new LambdaQueryWrapper<StockInfo>()
                        .eq(StockInfo::getCode, stockCode)
                        .eq(StockInfo::getIsDeleted, false)
                        .orderByDesc(StockInfo::getSnapshotDate)
                        .last("LIMIT 1"));
        if (info == null) return vo;

        vo.setStockName(info.getName());

        StockDetailVO.LatestQuote quote = new StockDetailVO.LatestQuote();
        quote.setClose(info.getLatestPrice());
        quote.setChangePct(info.getChangePct());
        quote.setVolume(info.getVolume());
        vo.setLatestQuote(quote);

        StockIndustry industry = stockIndustryMapper.selectOne(
                new LambdaQueryWrapper<StockIndustry>()
                        .eq(StockIndustry::getStockCode, stockCode)
                        .eq(StockIndustry::getIsDeleted, false)
                        .orderByDesc(StockIndustry::getSnapDate)
                        .last("LIMIT 1"));
        if (industry != null) vo.setIndustry(industry.getIndustryCode());

        List<StockConcept> concepts = stockConceptMapper.selectList(
                new LambdaQueryWrapper<StockConcept>()
                        .eq(StockConcept::getStockCode, stockCode)
                        .eq(StockConcept::getIsDeleted, false)
                        .orderByDesc(StockConcept::getSnapDate)
                        .last("LIMIT 10"));
        vo.setConcepts(concepts.stream().map(StockConcept::getConceptCode).collect(Collectors.toList()));

        List<StockDaily> dailies;
        if (startDate != null && endDate != null) {
            dailies = stockDailyMapper.selectList(
                    new LambdaQueryWrapper<StockDaily>()
                            .eq(StockDaily::getStockCode, stockCode)
                            .between(StockDaily::getTradeDate, startDate, endDate)
                            .eq(StockDaily::getIsDeleted, false)
                            .orderByAsc(StockDaily::getTradeDate));
        } else {
            dailies = stockDailyMapper.selectList(
                    new LambdaQueryWrapper<StockDaily>()
                            .eq(StockDaily::getStockCode, stockCode)
                            .eq(StockDaily::getIsDeleted, false)
                            .orderByDesc(StockDaily::getTradeDate)
                            .last("LIMIT 30"));
        }

        List<StockDetailVO.DailyKline> klines = new ArrayList<>();
        for (StockDaily sd : dailies) {
            StockDetailVO.DailyKline k = new StockDetailVO.DailyKline();
            k.setTradeDate(sd.getTradeDate());
            k.setOpen(sd.getOpen());
            k.setHigh(sd.getHigh());
            k.setLow(sd.getLow());
            k.setClose(sd.getClose());
            k.setVolume(sd.getVolume());
            klines.add(k);
        }
        vo.setDailyKlines(klines);

        List<MarginDaily> margins = marginDailyMapper.selectList(
                new LambdaQueryWrapper<MarginDaily>()
                        .eq(MarginDaily::getStockCode, stockCode)
                        .eq(MarginDaily::getIsDeleted, false)
                        .orderByDesc(MarginDaily::getTradeDate)
                        .last("LIMIT 30"));
        List<StockDetailVO.DailyMargin> dmList = new ArrayList<>();
        for (MarginDaily md : margins) {
            StockDetailVO.DailyMargin dm = new StockDetailVO.DailyMargin();
            dm.setTradeDate(md.getTradeDate());
            dm.setMarginBalance(md.getMarginBalance());
            dm.setMarginChange(md.getMarginChange());
            dm.setShortBalance(md.getShortBalance());
            dm.setShortChange(md.getShortChange());
            dmList.add(dm);
        }
        vo.setDailyMargins(dmList);

        if (!margins.isEmpty()) {
            MarginDaily latest = margins.get(0);
            StockDetailVO.LatestMargin lm = new StockDetailVO.LatestMargin();
            lm.setMarginBalance(latest.getMarginBalance());
            lm.setMarginBuy(latest.getMarginBuy());
            lm.setShortBalance(latest.getShortBalance());
            lm.setTotalBalance(latest.getTotalBalance());
            vo.setLatestMargin(lm);
        }

        if (!dailies.isEmpty()) {
            StockDaily latestDay = dailies.get(dailies.size() - 1);
            quote.setOpen(latestDay.getOpen());
            quote.setHigh(latestDay.getHigh());
            quote.setLow(latestDay.getLow());
        }

        return vo;
    }
}
