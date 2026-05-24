package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.DataCollectionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DataCollectionLogMapper extends BaseMapper<DataCollectionLog> {

    DataCollectionLog selectLatestByDataTypeAndJobType(@Param("dataType") String dataType, @Param("jobType") String jobType);

    List<DataCollectionLog> selectRecentByDataType(@Param("dataType") String dataType, @Param("limit") int limit);

    DataCollectionLog selectLatestByDataTypeAndJobTypeAndTradeDate(@Param("dataType") String dataType, @Param("jobType") String jobType, @Param("tradeDate") LocalDate tradeDate);

    List<DataCollectionLog> selectRecent(@Param("limit") int limit);
}
