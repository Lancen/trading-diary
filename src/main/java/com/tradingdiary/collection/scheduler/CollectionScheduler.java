package com.tradingdiary.collection.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tradingdiary.collection.orchestrator.CollectionOrchestrator;
import com.tradingdiary.entity.RawData;
import com.tradingdiary.entity.TradeCalendar;
import com.tradingdiary.mapper.RawDataMapper;
import com.tradingdiary.mapper.TradeCalendarMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * 数据采集调度器，定时触发股票行情、板块分类和两融数据的采集与清洗任务。
 * 全部定时任务已暂停（@Scheduled 已注释），当前阶段使用手动触发。
 * 需要恢复时，取消注释 @Scheduled 注解即可。
 */
@Component
@Profile("!test")
public class CollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);

    private final CollectionOrchestrator orchestrator;
    private final TradeCalendarMapper tradeCalendarMapper;
    private final RawDataMapper rawDataMapper;

    public CollectionScheduler(CollectionOrchestrator orchestrator,
                               TradeCalendarMapper tradeCalendarMapper,
                               RawDataMapper rawDataMapper) {
        this.orchestrator = orchestrator;
        this.tradeCalendarMapper = tradeCalendarMapper;
        this.rawDataMapper = rawDataMapper;
    }

    /**
     * 工作日 16:00：采集股票基础信息和日线 OHLCV 数据。
     * 已暂停定时执行，改为手动触发。
     */
    // @Scheduled(cron = "0 0 16 * * MON-FRI")
    public void collectStockData() {
        LocalDate today = LocalDate.now();
        if (!isTradeDay(today)) {
            log.info("Skipping stock data collection: {} is not a trade day", today);
            return;
        }
        log.info("Scheduled stock data collection for {}", today);

        String result1 = orchestrator.orchestrate("STOCK_INFO", today);
        log.info("STOCK_INFO orchestration result: {}", result1);

        String result2 = orchestrator.orchestrate("STOCK_DAILY", today);
        log.info("STOCK_DAILY orchestration result: {}", result2);
    }

    /**
     * 工作日 17:00：采集行业和概念板块分类数据。
     * 已暂停定时执行，改为手动触发。
     */
    // @Scheduled(cron = "0 0 17 * * MON-FRI")
    public void collectClassificationData() {
        LocalDate today = LocalDate.now();
        if (!isTradeDay(today)) {
            log.info("Skipping classification data collection: {} is not a trade day", today);
            return;
        }
        log.info("Scheduled classification data collection for {}", today);

        String r1 = orchestrator.orchestrate("INDUSTRY_NAME", today);
        log.info("INDUSTRY_NAME orchestration result: {}", r1);

        String r2 = orchestrator.orchestrate("INDUSTRY_CONS", today);
        log.info("INDUSTRY_CONS orchestration result: {}", r2);

        String r3 = orchestrator.orchestrate("CONCEPT_NAME", today);
        log.info("CONCEPT_NAME orchestration result: {}", r3);

        String r4 = orchestrator.orchestrate("CONCEPT_CONS", today);
        log.info("CONCEPT_CONS orchestration result: {}", r4);
    }

    /**
     * 工作日 18:00：采集上交所和深交所两融明细数据。
     * 已暂停定时执行，改为手动触发。
     */
    // @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void collectMarginData() {
        LocalDate today = LocalDate.now();
        if (!isTradeDay(today)) {
            log.info("Skipping margin data collection: {} is not a trade day", today);
            return;
        }
        log.info("Scheduled margin data collection for {}", today);

        String r1 = orchestrator.orchestrate("MARGIN_DAILY_SSE", today);
        log.info("MARGIN_DAILY_SSE orchestration result: {}", r1);

        String r2 = orchestrator.orchestrate("MARGIN_DAILY_SZSE", today);
        log.info("MARGIN_DAILY_SZSE orchestration result: {}", r2);
    }

    /**
     * 每月归档任务：每月 1 日凌晨 3:00 执行。
     * 将 30 天前的 raw_data 记录归档为 GZIP 压缩的 JSON Lines 文件，
     * 然后删除已归档的记录。
     * 已暂停定时执行，改为手动触发。
     */
    // @Scheduled(cron = "0 0 3 1 * *")
    public void archiveOldRawData() {
        log.info("Starting monthly raw data archive job");
        LocalDate cutoffDate = LocalDate.now().minusDays(30);

        List<RawData> oldRecords = rawDataMapper.selectList(
                new LambdaQueryWrapper<RawData>()
                        .lt(RawData::getCreatedAt, cutoffDate.atStartOfDay())
        );

        if (oldRecords.isEmpty()) {
            log.info("No raw data records older than {} to archive", cutoffDate);
            return;
        }

        String month = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Path backupDir = Paths.get("backups", "raw_data");
        Path backupFile = backupDir.resolve(month + ".json.gz");

        try {
            Files.createDirectories(backupDir);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(backupFile))))) {
                for (RawData record : oldRecords) {
                    String rawJson = record.getRawJson();
                    if (rawJson != null) {
                        writer.write(rawJson);
                        writer.newLine();
                    }
                }
            }

            // 删除已归档的记录
            List<Long> ids = oldRecords.stream()
                    .map(RawData::getId)
                    .toList();
            rawDataMapper.deleteByIds(ids);

            log.info("Archived {} raw_data records to {} and deleted", oldRecords.size(), backupFile);
        } catch (Exception e) {
            log.error("Failed to archive raw data to {}", backupFile, e);
        }
    }

    /**
     * 通过查询交易日历判断给定日期是否为交易日。
     */
    private boolean isTradeDay(LocalDate date) {
        Long count = tradeCalendarMapper.selectCount(
                new LambdaQueryWrapper<TradeCalendar>()
                        .eq(TradeCalendar::getTradeDate, date)
                        .eq(TradeCalendar::getIsTradingDay, 1)
        );
        return count != null && count > 0;
    }
}
