package com.tradingdiary.collection.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.IndustryMapper;
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
 * 行业指数日线采集处理器，遍历所有行业逐个调 API 采集和清洗
 */
@Component
public class IndustryIndexDailyHandler implements SectorIndexHandler {

    private static final Logger log = LoggerFactory.getLogger(IndustryIndexDailyHandler.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AKToolsClient aktoolsClient;
    private final SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private final IndustryMapper industryMapper;
    private final RawDataMapper rawDataMapper;

    public IndustryIndexDailyHandler(AKToolsClient aktoolsClient,
                                      SectorIndexDailyCleanseService sectorIndexDailyCleanseService,
                                      IndustryMapper industryMapper,
                                      RawDataMapper rawDataMapper) {
        this.aktoolsClient = aktoolsClient;
        this.sectorIndexDailyCleanseService = sectorIndexDailyCleanseService;
        this.industryMapper = industryMapper;
        this.rawDataMapper = rawDataMapper;
    }

    @Override
    public String dataType() {
        return "INDUSTRY_INDEX_DAILY";
    }

    @Override
    public String fetch(LocalDate tradeDate) {
        throw new UnsupportedOperationException(
                "INDUSTRY_INDEX_DAILY uses fetchSectors() path, not standard fetch()");
    }

    @Override
    public int fetchSectors(LocalDate startDate, LocalDate endDate, Long collectionLogId) {
        String startStr = startDate != null ? startDate.format(YMD) : "";
        String endStr = endDate != null ? endDate.format(YMD) : "";

        List<Industry> sectors = industryMapper.selectList(
                new LambdaQueryWrapper<Industry>().eq(Industry::getIsDeleted, false));
        if (sectors.isEmpty()) {
            throw new IllegalStateException("行业表为空，请先采集行业名称（INDUSTRY_NAME）");
        }

        int total = 0;
        for (Industry sector : sectors) {
            try {
                String rawJson = aktoolsClient.fetchIndustryIndexDaily(sector.getName(), startStr, endStr);
                if (rawJson != null && !rawJson.equals("[]")) {
                    saveSectorRawData(collectionLogId, endDate, sector.getCode(), rawJson);
                    total++;
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to fetch industry index daily for {}: {}", sector.getCode(), e.getMessage());
            }
        }
        return total;
    }

    @Override
    public int cleanse(String rawJson, LocalDate tradeDate) {
        // 优先从 raw_data 表读取已采集的数据
        List<RawData> rawDatas = rawDataMapper.selectList(
                new LambdaQueryWrapper<RawData>()
                        .eq(RawData::getDataType, "INDUSTRY_INDEX_DAILY")
                        .eq(RawData::getTradeDate, tradeDate)
                        .isNotNull(RawData::getSectorCode));

        if (rawDatas.isEmpty()) {
            log.warn("No raw_data found for INDUSTRY_INDEX_DAILY on {}, falling back to direct API fetch", tradeDate);
            return cleanseDirect(tradeDate);
        }

        int total = 0;
        for (RawData rd : rawDatas) {
            try {
                String json = rd.getRawJson();
                if (json != null && !json.equals("[]")) {
                    total += sectorIndexDailyCleanseService.cleanse(json, "INDUSTRY", rd.getSectorCode());
                }
            } catch (Exception e) {
                log.error("Failed to cleanse industry index daily for {}: {}", rd.getSectorCode(), e.getMessage());
            }
        }
        return total;
    }

    // 无 raw_data 时的 fallback：直接调 API 清洗
    private int cleanseDirect(LocalDate tradeDate) {
        String startDate = tradeDate != null ? tradeDate.format(YMD) : "";
        String endDate = startDate;

        List<Industry> sectors = industryMapper.selectList(
                new LambdaQueryWrapper<Industry>().eq(Industry::getIsDeleted, false));
        int total = 0;
        for (Industry sector : sectors) {
            try {
                String rawJson = aktoolsClient.fetchIndustryIndexDaily(sector.getName(), startDate, endDate);
                if (rawJson != null && !rawJson.equals("[]")) {
                    total += sectorIndexDailyCleanseService.cleanse(rawJson, "INDUSTRY", sector.getCode());
                }
                aktoolsClient.sleepBetweenCalls();
            } catch (Exception e) {
                log.error("Failed to cleanse industry index daily for {}: {}", sector.getCode(), e.getMessage());
            }
        }
        return total;
    }

    private void saveSectorRawData(Long collectionLogId, LocalDate tradeDate, String sectorCode, String rawJson) {
        RawData rawData = new RawData();
        rawData.setCollectionLogId(collectionLogId);
        rawData.setDataType("INDUSTRY_INDEX_DAILY");
        rawData.setTradeDate(tradeDate);
        rawData.setSource("AKTools");
        rawData.setSectorCode(sectorCode);
        rawData.setRawJson(rawJson);
        rawData.setFetchAt(LocalDateTime.now());
        rawDataMapper.insert(rawData);
    }
}