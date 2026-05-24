package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.StockConcept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票-概念关联 Mapper，提供股票与概念板块映射关系的基础 CRUD 操作
 */
@Mapper
public interface StockConceptMapper extends BaseMapper<StockConcept> {
}
