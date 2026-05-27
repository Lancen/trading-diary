package com.tradingdiary.collection.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.service.collection.SectorIndexDailyCleanseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 概念指数日线采集处理器，遍历所有概念逐个调 API 采集和清洗
 */
@Component
public class ConceptIndexDailyHandler implements SectorIndexHandler {

    private static final Logger log = LoggerFactory.getLogger(ConceptIndexDailyHandler.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AKToolsClient aktoolsClient;
    private final SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private final ConceptMapper conceptMapper;
    private final RawDataMapper rawDataMapper;

    public ConceptIndexDailyHandler(AKToolsClient aktoolsClient,
                                     SectorIndexDailyCleanseService sectorIndexDailyCleanseService,
                                     ConceptMapper conceptMapper,
                                     RawDataMapper rawDataMapper) {
        this.aktoolsClient = aktoolsClient;
        this.sectorIndexDailyCleanseService = sectorIndexDailyCleanseService;
        this.conceptMapper = conceptMapper;
        this.rawDataMapper = rawDataMapper;
    }

    @Override
    public String dataType() {
        return "CONCEPT_INDEX_DAILY";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        throw new UnsupportedOperationException(
                "CONCEPT_INDEX_DAILY uses fetchSectors() path, not standard fetch()");
    }

    @Override
    public int fetchSectors(LocalDate startDate, LocalDate endDate, Long collectionLogId) {
        String startStr = startDate != null ? startDate.format(YMD) : "";
        String endStr = endDate != null ? endDate.format(YMD) : "";

        List<Concept> sectors = conceptMapper.selectList(
                new LambdaQueryWrapper<Concept>().eq(Concept::getIsDeleted, false));
        if (sectors.isEmpty()) {
            throw new IllegalStateException("概念表为空，请先采集概念名称（CONCEPT_NAME）");
        }

        int total = 0;
        for (Concept sector : sectors) {
            try {
                String rawJson = aktoolsClient.fetchConceptIndexDaily(sector.getName(), startStr, endStr);
                if (rawJson != null && !rawJson.equals("[]")) {
                    saveSectorRawData(collectionLogId, endDate, sector.getCode(), rawJson);
                    total++;
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to fetch concept index daily for {}: {}", sector.getCode(), e.getMessage());
            }
        }
        return total;
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        List<RawData> rawDatas = rawDataMapper.selectList(
                new LambdaQueryWrapper<RawData>()
                        .eq(RawData::getDataType, "CONCEPT_INDEX_DAILY")
                        .eq(RawData::getTradeDate, tradeDate)
                        .isNotNull(RawData::getSectorCode));

        if (rawDatas.isEmpty()) {
            log.warn("No raw_data found for CONCEPT_INDEX_DAILY on {}, falling back to direct API fetch", tradeDate);
            return cleanseDirect(tradeDate);
        }

        int total = 0;
        for (RawData rd : rawDatas) {
            try {
                String json = rd.getRawJson();
                if (json != null && !json.equals("[]")) {
                    total += sectorIndexDailyCleanseService.cleanse(json, "CONCEPT", rd.getSectorCode());
                }
            } catch (Exception e) {
                log.error("Failed to cleanse concept index daily for {}: {}", rd.getSectorCode(), e.getMessage());
            }
        }
        return total;
    }

    private int cleanseDirect(LocalDate tradeDate) {
        String startDate = tradeDate != null ? tradeDate.format(YMD) : "";
        String endDate = startDate;

        List<Concept> sectors = conceptMapper.selectList(
                new LambdaQueryWrapper<Concept>().eq(Concept::getIsDeleted, false));
        int total = 0;
        for (Concept sector : sectors) {
            try {
                String rawJson = aktoolsClient.fetchConceptIndexDaily(sector.getName(), startDate, endDate);
                if (rawJson != null && !rawJson.equals("[]")) {
                    total += sectorIndexDailyCleanseService.cleanse(rawJson, "CONCEPT", sector.getCode());
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to cleanse concept index daily for {}: {}", sector.getCode(), e.getMessage());
            }
        }
        return total;
    }

    private void saveSectorRawData(Long collectionLogId, LocalDate tradeDate, String sectorCode, String rawJson) {
        RawData rawData = new RawData();
        rawData.setCollectionLogId(collectionLogId);
        rawData.setDataType("CONCEPT_INDEX_DAILY");
        rawData.setTradeDate(tradeDate);
        rawData.setSource("AKTools");
        rawData.setSectorCode(sectorCode);
        rawData.setRawJson(rawJson);
        rawData.setFetchAt(LocalDateTime.now());
        rawDataMapper.insert(rawData);
    }
}