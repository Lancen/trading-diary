package com.tradingdiary.security;

import com.tradingdiary.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 令牌工具，负责令牌签发、验证和解析
 */
@Component
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    private SecretKey secretKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发访问令牌
     *
     * @param userId 用户 ID
     * @param roles  用户角色列表
     * @return 访问令牌字符串
     */
    public String issueAccessToken(Long userId, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessExpiration() * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 签发刷新令牌
     *
     * @param userId 用户 ID
     * @return 刷新令牌字符串
     */
    public String issueRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getRefreshExpiration() * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Long getAccessExpiration() {
        return jwtConfig.getAccessExpiration();
    }

    public Long getRefreshExpiration() {
        return jwtConfig.getRefreshExpiration();
    }

    /**
     * 验证令牌是否有效（签名合法且未过期）
     *
     * @param token 待验证令牌
     * @return 有效返回 true，否则返回 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从令牌中解析用户 ID
     *
     * @param token JWT 令牌
     * @return 用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从令牌中解析用户角色列表
     *
     * @param token JWT 令牌
     * @return 角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

    /**
     * 从令牌中解析令牌类型（access/refresh）
     *
     * @param token JWT 令牌
     * @return 令牌类型字符串
     */
    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("type", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
