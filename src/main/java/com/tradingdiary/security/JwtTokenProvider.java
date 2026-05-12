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

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

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
