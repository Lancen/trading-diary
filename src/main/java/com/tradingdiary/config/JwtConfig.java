package com.tradingdiary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置，定义令牌密钥和过期时间参数
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /** 令牌签名密钥 */
    private String secret;

    /** 访问令牌过期时间（秒） */
    private Long accessExpiration;

    /** 刷新令牌过期时间（秒） */
    private Long refreshExpiration;
}
