package com.tradingdiary.service.collection;

/**
 * 宽基指数日线清洗服务，解析指数 OHLCV JSON 并入库
 */
public interface MarketIndexDailyCleanseService {

    /**
     * 清洗宽基指数日线原始 JSON 数据
     *
     * @param rawJson   采集返回的原始 JSON 字符串
     * @param indexCode 指数代码
     * @return 成功入库的记录数
     */
    int cleanse(String rawJson, String indexCode);
}
