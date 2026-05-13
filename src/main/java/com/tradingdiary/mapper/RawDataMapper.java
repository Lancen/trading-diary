package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.RawData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RawDataMapper extends BaseMapper<RawData> {
}
