package com.tradingdiary.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * JSON 节点安全读取工具，提供 null 安全的类型转换方法
 */
public final class JsonNodeHelper {

    private static final Logger log = LoggerFactory.getLogger(JsonNodeHelper.class);

    private JsonNodeHelper() {}

    /** 安全读取文本字段，null/缺失返回 null */
    public static String safeText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }

    /** 安全读取数组索引处的文本，null/缺失返回 null */
    public static String safeText(JsonNode node, int index) {
        if (!node.has(index)) return null;
        JsonNode element = node.get(index);
        if (element == null || element.isNull()) return null;
        return element.asText();
    }

    /** 安全读取十进制数字段，null/缺失/空/"-" 返回 null */
    public static BigDecimal safeDecimal(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        try {
            String text = fieldNode.asText();
            if (text == null || text.isEmpty() || "-".equals(text)) return null;
            return new BigDecimal(text);
        } catch (Exception e) {
            log.debug("解析BigDecimal失败: field={}", field, e.getMessage());
            return null;
        }
    }

    /** 安全读取数组索引处的十进制数，null/缺失/空/"-" 返回 null */
    public static BigDecimal safeDecimal(JsonNode node, int index) {
        if (!node.has(index)) return null;
        JsonNode element = node.get(index);
        if (element == null || element.isNull()) return null;
        try {
            String text = element.asText();
            if (text == null || text.isEmpty() || "-".equals(text)) return null;
            return new BigDecimal(text);
        } catch (Exception e) {
            log.debug("解析BigDecimal失败: index={}", index, e.getMessage());
            return null;
        }
    }

    /** 安全读取长整数字段，null/缺失/空/"-" 返回 null */
    public static Long safeLong(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        try {
            String text = fieldNode.asText();
            if (text == null || text.isEmpty() || "-".equals(text)) return null;
            return Long.parseLong(text);
        } catch (Exception e) {
            log.debug("解析Long失败: field={}", field, e.getMessage());
            return null;
        }
    }

    /** 安全读取数组索引处的长整数，null/缺失/空/"-" 返回 null */
    public static Long safeLong(JsonNode node, int index) {
        if (!node.has(index)) return null;
        JsonNode element = node.get(index);
        if (element == null || element.isNull()) return null;
        try {
            String text = element.asText();
            if (text == null || text.isEmpty() || "-".equals(text)) return null;
            return Long.parseLong(text);
        } catch (Exception e) {
            log.debug("解析Long失败: index={}", index, e.getMessage());
            return null;
        }
    }

    /** 剥离股票代码的市场前缀（sh/sz/bj），如 "sh600000" → "600000" */
    public static String stripMarketPrefix(String code) {
        if (code == null || code.isEmpty()) return code;
        if (code.length() > 2) {
            String prefix = code.substring(0, 2).toLowerCase();
            if ("sh".equals(prefix) || "sz".equals(prefix) || "bj".equals(prefix)) {
                return code.substring(2);
            }
        }
        return code;
    }
}