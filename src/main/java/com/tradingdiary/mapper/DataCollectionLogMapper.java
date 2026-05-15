package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.DataCollectionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DataCollectionLogMapper extends BaseMapper<DataCollectionLog> {

    @Select("SELECT * FROM data_collection_log WHERE data_type = #{dataType} AND job_type = #{jobType} ORDER BY id DESC LIMIT 1")
    DataCollectionLog selectLatestByDataTypeAndJobType(@Param("dataType") String dataType, @Param("jobType") String jobType);

    @Select("SELECT * FROM data_collection_log WHERE data_type = #{dataType} ORDER BY id DESC LIMIT #{limit}")
    List<DataCollectionLog> selectRecentByDataType(@Param("dataType") String dataType, @Param("limit") int limit);

    @Select("SELECT * FROM data_collection_log WHERE data_type = #{dataType} AND job_type = #{jobType} AND trade_date = #{tradeDate} ORDER BY id DESC LIMIT 1")
    DataCollectionLog selectLatestByDataTypeAndJobTypeAndTradeDate(@Param("dataType") String dataType, @Param("jobType") String jobType, @Param("tradeDate") LocalDate tradeDate);

    @Select("SELECT * FROM data_collection_log ORDER BY id DESC LIMIT #{limit}")
    List<DataCollectionLog> selectRecent(@Param("limit") int limit);
}
