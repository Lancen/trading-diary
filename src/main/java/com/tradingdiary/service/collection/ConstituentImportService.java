package com.tradingdiary.service.collection;

import java.util.List;
import java.util.Map;

/**
 * 成分股导入服务，封装成分股文件的扫描与导入逻辑
 */
public interface ConstituentImportService {

    /**
     * 列出可导入的成分股文件
     *
     * @return 文件信息列表，包含文件名和大小等
     */
    List<Map<String, Object>> listFiles();

    /**
     * 从指定文件导入成分股数据
     *
     * @param filename 文件名
     * @return 导入结果，包含成功和失败的记录数
     */
    Map<String, Object> importFromFile(String filename);
}
