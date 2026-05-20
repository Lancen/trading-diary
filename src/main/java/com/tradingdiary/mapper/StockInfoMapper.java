package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.collection.model.StockListVO;
import com.tradingdiary.entity.StockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockInfoMapper extends BaseMapper<StockInfo> {

    @Select("<script>" +
            "SELECT si.code AS stockCode, si.name AS stockName, " +
            "si.latest_price AS close, si.change_pct AS changePct, " +
            "si.volume, si.snapshot_date AS tradeDate, " +
            "ind.name AS industry, " +
            "GROUP_CONCAT(DISTINCT c.name ORDER BY c.name SEPARATOR ',') AS concepts, " +
            "md.margin_balance AS marginBalance, md.margin_change AS marginChange, " +
            "md.short_balance AS shortBalance, md.short_change AS shortChange " +
            "FROM stock_info si " +
            "LEFT JOIN stock_industry sci ON si.code = sci.stock_code AND si.snapshot_date = sci.snap_date AND sci.is_deleted = 0 " +
            "LEFT JOIN industry ind ON sci.industry_code = ind.code AND ind.is_deleted = 0 " +
            "LEFT JOIN stock_concept sc ON si.code = sc.stock_code AND si.snapshot_date = sc.snap_date AND sc.is_deleted = 0 " +
            "LEFT JOIN concept c ON sc.concept_code = c.code AND c.is_deleted = 0 " +
            "LEFT JOIN margin_daily md ON si.code = md.stock_code AND si.snapshot_date = md.trade_date AND md.is_deleted = 0 " +
            "WHERE si.is_deleted = 0 " +
            "<if test='tradeDate != null'>AND si.snapshot_date = #{tradeDate}</if>" +
            "<if test='tradeDate == null'>AND si.snapshot_date = (SELECT MAX(snapshot_date) FROM stock_info WHERE is_deleted = 0)</if>" +
            "<if test='keyword != null and keyword != \"\"'>AND (si.code LIKE CONCAT('%', #{keyword}, '%') OR si.name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='industry != null and industry != \"\"'>AND ind.name = #{industry}</if>" +
            "<if test='concept != null and concept != \"\"'>AND c.name = #{concept}</if>" +
            "GROUP BY si.code, si.name, si.latest_price, si.change_pct, si.volume, si.snapshot_date, ind.name, md.margin_balance, md.margin_change, md.short_balance, md.short_change " +
            "ORDER BY ${sortBy} ${sortDir} " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<StockListVO> selectStockList(@Param("keyword") String keyword,
                                      @Param("industry") String industry,
                                      @Param("concept") String concept,
                                      @Param("tradeDate") String tradeDate,
                                      @Param("sortBy") String sortBy,
                                      @Param("sortDir") String sortDir,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT si.id) " +
            "FROM stock_info si " +
            "LEFT JOIN stock_industry sci ON si.code = sci.stock_code AND si.snapshot_date = sci.snap_date AND sci.is_deleted = 0 " +
            "LEFT JOIN industry ind ON sci.industry_code = ind.code AND ind.is_deleted = 0 " +
            "LEFT JOIN stock_concept sc ON si.code = sc.stock_code AND si.snapshot_date = sc.snap_date AND sc.is_deleted = 0 " +
            "LEFT JOIN concept c ON sc.concept_code = c.code AND c.is_deleted = 0 " +
            "WHERE si.is_deleted = 0 " +
            "<if test='tradeDate != null'>AND si.snapshot_date = #{tradeDate}</if>" +
            "<if test='tradeDate == null'>AND si.snapshot_date = (SELECT MAX(snapshot_date) FROM stock_info WHERE is_deleted = 0)</if>" +
            "<if test='keyword != null and keyword != \"\"'>AND (si.code LIKE CONCAT('%', #{keyword}, '%') OR si.name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='industry != null and industry != \"\"'>AND ind.name = #{industry}</if>" +
            "<if test='concept != null and concept != \"\"'>AND c.name = #{concept}</if>" +
            "</script>")
    long countStockList(@Param("keyword") String keyword,
                        @Param("industry") String industry,
                        @Param("concept") String concept,
                        @Param("tradeDate") String tradeDate);
}
