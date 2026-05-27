package com.tradingdiary.collection.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.collection.client.AKToolsClient;
import com.tradingdiary.collection.model.FetchResult;
import com.tradingdiary.entity.DataCollectionLog;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.mapper.DataCollectionLogMapper;
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
public class IndustryIndexDailyHandler implements DataTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(IndustryIndexDailyHandler.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AKToolsClient aktoolsClient;
    private final SectorIndexDailyCleanseService sectorIndexDailyCleanseService;
    private final IndustryMapper industryMapper;
    private final RawDataMapper rawDataMapper;
    private final DataCollectionLogMapper logMapper;

    public IndustryIndexDailyHandler(AKToolsClient aktoolsClient,
                                      SectorIndexDailyCleanseService sectorIndexDailyCleanseService,
                                      IndustryMapper industryMapper,
                                      RawDataMapper rawDataMapper,
                                      DataCollectionLogMapper logMapper) {
        this.aktoolsClient = aktoolsClient;
        this.sectorIndexDailyCleanseService = sectorIndexDailyCleanseService;
        this.industryMapper = industryMapper;
        this.rawDataMapper = rawDataMapper;
        this.logMapper = logMapper;
    }

    @Override
    public String dataType() {
        return "INDUSTRY_INDEX_DAILY";
    }

    @Override
    public FetchResult fetch(LocalDate tradeDate) {
        // 创建 FETCH 日志
        DataCollectionLog fetchLog = new DataCollectionLog();
        fetchLog.setDataType("INDUSTRY_INDEX_DAILY");
        fetchLog.setJobType("FETCH");
        fetchLog.setTradeDate(tradeDate);
        fetchLog.setStatus("RUNNING");
        fetchLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(fetchLog);

        String startStr = tradeDate != null ? tradeDate.format(YMD) : "";
        String endStr = startStr;

        List<Industry> sectors = industryMapper.selectList(
                new LambdaQueryWrapper<Industry>().eq(Industry::getIsDeleted, false));
        if (sectors.isEmpty()) {
            fetchLog.setStatus("FAILED");
            fetchLog.setErrorMsg("行业表为空，请先采集行业名称（INDUSTRY_NAME）");
            fetchLog.setCompletedAt(LocalDateTime.now());
            logMapper.updateById(fetchLog);
            return FetchResult.single(null);
        }

        int successCount = 0;
        try {
            for (Industry sector : sectors) {
                try {
                    String rawJson = aktoolsClient.fetchIndustryIndexDaily(sector.getName(), startStr, endStr);
                    if (rawJson != null && !rawJson.equals("[]")) {
                        saveSectorRawData(fetchLog.getId(), tradeDate, sector.getCode(), rawJson);
                        successCount++;
                    }
                    aktoolsClient.sleepBetweenCalls();
                } catch (Exception e) {
                    log.error("Failed to fetch industry index daily for {}: {}", sector.getCode(), e.getMessage());
                }
            }
            fetchLog.setStatus("SUCCESS");
            fetchLog.setRecordCount(successCount);
        } catch (Exception e) {
            fetchLog.setStatus("FAILED");
            fetchLog.setErrorMsg(e.getMessage());
        }
        fetchLog.setCompletedAt(LocalDateTime.now());
        logMapper.updateById(fetchLog);

        return FetchResult.multiSector(fetchLog.getId(), successCount);
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