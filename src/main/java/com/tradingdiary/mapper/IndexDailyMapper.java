package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.IndexDaily;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指数日线行情 Mapper，提供指数日线数据的基础 CRUD 操作
 */
@Mapper
public interface IndexDailyMapper extends BaseMapper<IndexDaily> {
}
