package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.collection.model.ConceptIndustryVO;
import com.tradingdiary.entity.Industry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行业 Mapper，提供行业分类数据的查询与统计
 */
@Mapper
public interface IndustryMapper extends BaseMapper<Industry> {

    /**
     * 分页查询行业列表，包含融资融券汇总统计
     *
     * @param keyword  关键词（模糊匹配行业名称）
     * @param tradeDate 交易日期，为空时取最新融资融券日期
     * @param sortBy   排序字段
     * @param sortDir  排序方向（ASC 或 DESC）
     * @param offset   分页偏移量
     * @param size     每页条数
     * @return 行业列表视图对象
     */
    List<ConceptIndustryVO> selectIndustryList(@Param("keyword") String keyword,
                                               @Param("tradeDate") String tradeDate,
                                               @Param("sortBy") String sortBy,
                                               @Param("sortDir") String sortDir,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    /**
     * 统计符合筛选条件的行业总数
     *
     * @param keyword  关键词（模糊匹配行业名称）
     * @param tradeDate 交易日期，为空时取最新融资融券日期
     * @return 符合条件的行业数量
     */
    long countIndustryList(@Param("keyword") String keyword, @Param("tradeDate") String tradeDate);

    /**
     * 查询行业表中的最新创建时间
     *
     * @return 最大创建时间，无记录时返回 null
     */
    LocalDateTime selectMaxCreatedAt();
}
