package com.tradingdiary.service.collection;

import java.util.Map;

/**
 * 成分股抓取服务，通过 Playwright 抓取同花顺成分股数据
 */
public interface ConstituentScrapeService {

    /**
     * 同步抓取并导入成分股数据
     *
     * @param boardType 板块类型（industry/concept）
     * @param code      板块代码
     * @return 抓取结果，包含抓取数量等状态信息
     */
    Map<String, Object> scrapeAndImport(String boardType, String code);

    /**
     * 查询异步抓取任务的状态
     *
     * @param boardType 板块类型（industry/concept）
     * @param code      板块代码
     * @return 抓取状态信息
     */
    Map<String, Object> getScrapeStatus(String boardType, String code);

    /**
     * 启动异步抓取任务
     *
     * @param boardType 板块类型（industry/concept）
     * @param code      板块代码
     */
    void startAsyncScrape(String boardType, String code);
}
