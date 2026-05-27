package com.tradingdiary.service.market;

import com.tradingdiary.model.vo.SectorRanking;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 板块排名服务，提供行业/概念板块排名查询
 */
public interface SectorRankingService {

    /** 合法的板块类型 */
    Set<String> VALID_TYPES = Set.of("INDUSTRY", "CONCEPT");

    /** 合法的排序字段 */
    Set<String> VALID_SORT_FIELDS = Set.of("amount", "amountChange", "changePct", "changePctChange");

    /**
     * 查询板块排名
     *
     * @param sectorType 板块类型（INDUSTRY/CONCEPT）
     * @param tradeDate 交易日期，null 时取最新交易日
     * @param sortBy 排序字段（amount/amountChange/changePct/changePctChange）
     * @param sortDir 排序方向（asc/desc）
     * @return 板块排名列表
     */
    List<SectorRanking> query(String sectorType, LocalDate tradeDate, String sortBy, String sortDir);
}