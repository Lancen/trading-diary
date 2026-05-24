package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.DataCollectionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据采集日志 Mapper，提供采集任务的执行记录查询
 */
@Mapper
public interface DataCollectionLogMapper extends BaseMapper<DataCollectionLog> {

    /**
     * 查询指定数据类型和作业类型的最近一条采集日志
     *
     * @param dataType 数据类型（如 STOCK_INFO、MARGIN_DAILY_SSE）
     * @param jobType  作业类型（FETCH 或 CLEANSE）
     * @return 最近一条采集日志，无记录时返回 null
     */
    DataCollectionLog selectLatestByDataTypeAndJobType(@Param("dataType") String dataType, @Param("jobType") String jobType);

    /**
     * 查询指定数据类型的最近 N 条采集日志
     *
     * @param dataType 数据类型
     * @param limit    返回条数上限
     * @return 按ID倒序排列的采集日志列表
     */
    List<DataCollectionLog> selectRecentByDataType(@Param("dataType") String dataType, @Param("limit") int limit);

    /**
     * 查询指定数据类型、作业类型和交易日期的最近一条采集日志
     *
     * @param dataType  数据类型
     * @param jobType   作业类型（FETCH 或 CLEANSE）
     * @param tradeDate 交易日期
     * @return 最近一条采集日志，无记录时返回 null
     */
    DataCollectionLog selectLatestByDataTypeAndJobTypeAndTradeDate(@Param("dataType") String dataType, @Param("jobType") String jobType, @Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询最近 N 条采集日志（不限数据类型）
     *
     * @param limit 返回条数上限
     * @return 按ID倒序排列的采集日志列表
     */
    List<DataCollectionLog> selectRecent(@Param("limit") int limit);
}
