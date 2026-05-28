package com.tradingdiary.service.collection;

import java.util.Map;

/**
 * 系统配置服务接口，管理同花顺 Cookie 等运行时配置的读写
 */
public interface SystemConfigService {

    /**
     * 读取同花顺 Cookie
     *
     * @return Cookie 字符串，不存在或读取失败时返回空字符串
     */
    String getThsCookie();

    /**
     * 保存或清除同花顺 Cookie
     *
     * @param cookie Cookie 字符串，为空时删除已有 Cookie
     * @return 操作结果，包含 status/message/cookie/updatedAt
     */
    Map<String, Object> setThsCookie(String cookie);

    /**
     * 查询同花顺 Cookie 状态
     *
     * @return 状态信息，包含 hasCookie/cookiePreview/updatedAt
     */
    Map<String, Object> getThsCookieStatus();
}