package com.tradingdiary.service.collection;

/**
 * 板块指数日线清洗服务，解析板块 OHLCV JSON 并入库
 */
public interface SectorIndexDailyCleanseService {

    /**
     * 清洗板块指数日线原始 JSON 数据
     *
     * @param rawJson    采集返回的原始 JSON 字符串
     * @param sectorType 板块类型（industry/concept）
     * @param sectorCode 板块代码
     * @return 成功入库的记录数
     */
    int cleanse(String rawJson, String sectorType, String sectorCode);
}
