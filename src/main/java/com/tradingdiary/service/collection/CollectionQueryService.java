package com.tradingdiary.service.collection;

import com.tradingdiary.collection.model.CollectionStatusVO;
import com.tradingdiary.entity.DataCollectionLog;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据采集查询服务，封装采集状态、日志和交易日历等查询逻辑
 */
public interface CollectionQueryService {

    /**
     * 获取所有数据类型的采集状态
     *
     * @return 采集状态列表，包含每种数据类型的最近采集任务和数据日期
     */
    List<CollectionStatusVO> getCollectionStatus();

    /**
     * 获取指定数据类型的最近采集日志
     *
     * @param dataType 数据类型
     * @param limit 返回记录数
     * @return 采集日志列表，按时间倒序
     */
    List<DataCollectionLog> getRecentLogs(String dataType, int limit);

    /**
     * 获取最近的交易日
     *
     * @return 最近交易日日期，若无记录则返回当前日期
     */
    LocalDate getLatestTradeDate();

    /**
     * 判断给定的数据类型是否为已知的采集类型
     *
     * @param dataType 数据类型
     * @return 是否为已知类型
     */
    boolean isValidDataType(String dataType);
}
