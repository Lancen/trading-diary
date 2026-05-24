package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.entity.StockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票基本信息 Mapper，提供股票行情数据的查询与统计
 */
@Mapper
public interface StockInfoMapper extends BaseMapper<StockInfo> {

    /**
     * 分页查询股票列表，支持关键词、行业、概念筛选和排序
     *
     * @param keyword  关键词（模糊匹配股票代码或名称）
     * @param industry 行业名称（精确匹配）
     * @param concept  概念名称（精确匹配）
     * @param tradeDate 交易日期，为空时取最新快照日期
     * @param sortBy   排序字段
     * @param sortDir  排序方向（ASC 或 DESC）
     * @param offset   分页偏移量
     * @param size     每页条数
     * @return 股票列表视图对象
     */
    List<StockListVO> selectStockList(@Param("keyword") String keyword,
                                      @Param("industry") String industry,
                                      @Param("concept") String concept,
                                      @Param("tradeDate") String tradeDate,
                                      @Param("sortBy") String sortBy,
                                      @Param("sortDir") String sortDir,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    /**
     * 统计符合筛选条件的股票总数
     *
     * @param keyword  关键词（模糊匹配股票代码或名称）
     * @param industry 行业名称（精确匹配）
     * @param concept  概念名称（精确匹配）
     * @param tradeDate 交易日期，为空时取最新快照日期
     * @return 符合条件的股票数量
     */
    long countStockList(@Param("keyword") String keyword,
                        @Param("industry") String industry,
                        @Param("concept") String concept,
                        @Param("tradeDate") String tradeDate);

    /**
     * 查询指定日期范围内的所有快照日期
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 去重的快照日期列表，按日期升序排列
     */
    List<LocalDate> selectDistinctSnapshotDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 查询股票信息表的最大快照日期
     *
     * @return 最大快照日期，无记录时返回 null
     */
    LocalDate selectMaxTradeDate();
}
