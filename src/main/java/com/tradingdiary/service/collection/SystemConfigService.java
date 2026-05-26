package com.tradingdiary.service.collection;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemConfigService {

    private final Path configDir = Paths.get("data/config");
    private final Path cookieFile = configDir.resolve("ths-cookie.txt");

    public SystemConfigService() {
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建配置目录: " + configDir, e);
        }
    }

    public String getThsCookie() {
        try {
            if (Files.exists(cookieFile)) {
                return Files.readString(cookieFile).trim();
            }
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    public Map<String, Object> setThsCookie(String cookie) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (cookie == null || cookie.isBlank()) {
                Files.deleteIfExists(cookieFile);
                result.put("status", "success");
                result.put("message", "Cookie已清除");
                result.put("cookie", "");
            } else {
                Files.writeString(cookieFile, cookie.trim());
                result.put("status", "success");
                result.put("message", "Cookie已保存");
                result.put("cookie", maskCookie(cookie));
            }
            result.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (IOException e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getThsCookieStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            if (Files.exists(cookieFile)) {
                String cookie = Files.readString(cookieFile).trim();
                status.put("hasCookie", !cookie.isEmpty());
                status.put("cookiePreview", maskCookie(cookie));
                status.put("updatedAt", Files.getLastModifiedTime(cookieFile).toString());
            } else {
                status.put("hasCookie", false);
                status.put("cookiePreview", "");
                status.put("updatedAt", null);
            }
        } catch (IOException e) {
            status.put("hasCookie", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    private String maskCookie(String cookie) {
        if (cookie == null || cookie.isEmpty()) {
            return "";
        }
        int len = cookie.length();
        if (len <= 20) {
            return cookie.substring(0, 5) + "...";
        }
        return cookie.substring(0, 10) + "...(" + len + " chars)";
    }
}