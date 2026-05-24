package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.RawData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原始数据 Mapper，提供采集原始数据的基础 CRUD 操作
 */
@Mapper
public interface RawDataMapper extends BaseMapper<RawData> {
}
