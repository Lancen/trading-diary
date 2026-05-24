package com.tradingdiary.service;

import com.tradingdiary.collection.model.GapReportVO;

import java.time.LocalDate;

/**
 * 数据缺口检测服务，封装两融数据缺失日期的检测与报告逻辑
 */
public interface GapDetectionService {

    /**
     * 检测指定日期范围内的数据缺口
     * <p>
     * 对比交易日历和实际采集的两融数据，返回缺失的交易日数据。
     * 结果按周分组，便于查看每周的数据完整性。
     * </p>
     *
     * @param start 开始日期
     * @param end 结束日期
     * @param dataType 数据类型（MARGIN_DAILY_SSE/SZSE 或 MARGIN_MACRO_SSE/SZSE）
     * @return 数据缺口报告，包含每周的完整度统计和缺失日期列表
     */
    GapReportVO getGaps(LocalDate start, LocalDate end, String dataType);
}
