package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.StockIndustry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票-行业关联 Mapper，提供股票与行业分类映射关系的基础 CRUD 操作
 */
@Mapper
public interface StockIndustryMapper extends BaseMapper<StockIndustry> {
}
