package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SectorIndexDaily;
import com.tradingdiary.model.vo.SectorRanking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 板块指数日线 Mapper，提供板块排名、最大交易日期和前一交易日查询
 */
@Mapper
public interface SectorIndexDailyMapper extends BaseMapper<SectorIndexDaily> {

    /**
     * 查询指定板块类型的最大交易日期
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT）
     * @return 该类型的最大交易日期，无数据时返回 null
     */
    LocalDate selectMaxTradeDateByType(@Param("sectorType") String sectorType);

    /**
     * 查询指定板块类型的上一交易日
     *
     * @param sectorType 板块类型
     * @param tradeDate 参考交易日
     * @return 前一交易日，无数据时返回 null
     */
    LocalDate selectPrevTradeDateByType(@Param("sectorType") String sectorType,
                                         @Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询板块排名，关联板块名称和前一交易日数据，计算成交占比
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT，小写用于表名路由）
     * @param tradeDate 交易日期
     * @param prevDate 前一交易日（用于计算变动）
     * @param sortBy 排序字段（amount/amountChange/changePct/changePctChange）
     * @param sortDir 排序方向（asc/desc）
     * @return 板块排名列表
     */
    List<SectorRanking> selectSectorRanking(@Param("sectorType") String sectorType,
                                             @Param("tradeDate") LocalDate tradeDate,
                                             @Param("prevDate") LocalDate prevDate,
                                             @Param("sortBy") String sortBy,
                                             @Param("sortDir") String sortDir);
}