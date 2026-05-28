package com.tradingdiary.service.market;

import java.util.List;
import java.util.Map;

/**
 * 板块置顶服务接口，管理行业和概念板块的置顶状态和排序
 */
public interface PinService {

    /**
     * 切换板块置顶状态
     *
     * @param type   板块类型（industry 或 concept）
     * @param code   板块代码
     * @param pinned 是否置顶
     * @return 操作结果，包含 status/type/code/pinned
     */
    Map<String, Object> togglePin(String type, String code, boolean pinned);

    /**
     * 重新排序已置顶的板块
     *
     * @param type 板块类型（industry 或 concept）
     * @param codes 板块代码列表，按新顺序排列
     * @return 操作结果，包含 status/type/reordered
     */
    Map<String, Object> reorderPinned(String type, List<String> codes);
}