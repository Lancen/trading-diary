package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.entity.StockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockInfoMapper extends BaseMapper<StockInfo> {

    List<StockListVO> selectStockList(@Param("keyword") String keyword,
                                      @Param("industry") String industry,
                                      @Param("concept") String concept,
                                      @Param("tradeDate") String tradeDate,
                                      @Param("sortBy") String sortBy,
                                      @Param("sortDir") String sortDir,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    long countStockList(@Param("keyword") String keyword,
                        @Param("industry") String industry,
                        @Param("concept") String concept,
                        @Param("tradeDate") String tradeDate);
}
