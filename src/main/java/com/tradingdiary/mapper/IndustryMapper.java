package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.collection.model.ConceptIndustryVO;
import com.tradingdiary.entity.Industry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IndustryMapper extends BaseMapper<Industry> {

    List<ConceptIndustryVO> selectIndustryList(@Param("keyword") String keyword,
                                               @Param("tradeDate") String tradeDate,
                                               @Param("sortBy") String sortBy,
                                               @Param("sortDir") String sortDir,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    long countIndustryList(@Param("keyword") String keyword, @Param("tradeDate") String tradeDate);
}
