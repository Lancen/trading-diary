package com.tradingdiary.collection.handler;

import java.time.LocalDate;

/**
 * 板块指数采集处理器扩展接口
 * <p>
 * 板块指数（行业/概念）的 FETCH 需要遍历所有板块逐个调 API，
 * 保存 N 条 sector raw_data，不走标准单条 raw_data 流程。
 * Orchestrator 检测 handler instanceof SectorIndexHandler 时走专用路径。
 * </p>
 */
public interface SectorIndexHandler extends DataTypeHandler {

    /**
     * 遍历所有板块，逐个调 API 采集数据并保存到 raw_data 表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param collectionLogId FETCH 日志 ID，关联到 raw_data 的 collection_log_id
     * @return 成功保存的板块数
     */
    int fetchSectors(LocalDate startDate, LocalDate endDate, Long collectionLogId);
}