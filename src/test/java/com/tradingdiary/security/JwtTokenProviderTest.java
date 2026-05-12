package com.tradingdiary.security;

import com.tradingdiary.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "test-secret-key-for-unit-tests-minimum-256-bits-long!!";
    private static final Long ACCESS_EXPIRATION = 900L;
    private static final Long REFRESH_EXPIRATION = 604800L;

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfig.getSecret()).thenReturn(SECRET);
        lenient().when(jwtConfig.getAccessExpiration()).thenReturn(ACCESS_EXPIRATION);
        lenient().when(jwtConfig.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        jwtTokenProvider.init();
    }

    @Test
    void shouldIssueValidAccessToken() {
        String token = jwtTokenProvider.issueAccessToken(1L, List.of("ADMIN", "USER"));

        assertThat(token).isNotBlank();
        assertTrue(jwtTokenProvider.validateToken(token));

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("roles", List.class)).containsExactly("ADMIN", "USER");
    }

    @Test
    void shouldIssueValidRefreshToken() {
        String token = jwtTokenProvider.issueRefreshToken(2L);

        assertThat(token).isNotBlank();
        assertTrue(jwtTokenProvider.validateToken(token));

        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("refresh");
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(2L);
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        // Override to return negative expiration so token is already expired
        when(jwtConfig.getAccessExpiration()).thenReturn(-1L);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(jwtConfig);
        expiredProvider.init();

        String token = expiredProvider.issueAccessToken(1L, List.of("USER"));
        assertFalse(expiredProvider.validateToken(token));
    }

    @Test
    void shouldReturnFalseForTamperedToken() {
        String token = jwtTokenProvider.issueAccessToken(1L, List.of("USER"));

        // Tamper with the token by appending characters
        String tamperedToken = token + "tampered";
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    void shouldReturnCorrectUserId() {
        String token = jwtTokenProvider.issueAccessToken(42L, List.of("USER"));

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void shouldReturnCorrectRoles() {
        String token = jwtTokenProvider.issueAccessToken(1L, List.of("ADMIN", "USER", "TRADER"));

        List<String> roles = jwtTokenProvider.getRoles(token);
        assertThat(roles).containsExactly("ADMIN", "USER", "TRADER");
    }

    @Test
    void shouldReturnCorrectEmptyRoles() {
        String token = jwtTokenProvider.issueAccessToken(1L, List.of());

        List<String> roles = jwtTokenProvider.getRoles(token);
        assertThat(roles).isEmpty();
    }

    @Test
    void refreshTokenRotationDemonstratesStatelessNature() {
        // Issue first refresh token
        String firstToken = jwtTokenProvider.issueRefreshToken(100L);
        assertTrue(jwtTokenProvider.validateToken(firstToken));
        assertThat(jwtTokenProvider.getUserId(firstToken)).isEqualTo(100L);

        // Issue a new refresh token (simulating rotation)
        String secondToken = jwtTokenProvider.issueRefreshToken(100L);
        assertTrue(jwtTokenProvider.validateToken(secondToken));

        // JWT is stateless — the old token is still cryptographically valid
        // This demonstrates why DB-layer revocation (in AuthServiceImpl) is needed
        assertTrue(jwtTokenProvider.validateToken(firstToken),
                "Old refresh token is still valid — demonstrates need for DB-level revocation");
        assertTrue(jwtTokenProvider.validateToken(secondToken));
    }

    @Test
    void shouldRejectAccessTokenWhenValidatingAsRefresh() {
        String accessToken = jwtTokenProvider.issueAccessToken(1L, List.of("USER"));

        // Access token's type claim is null (not "refresh")
        String tokenType = jwtTokenProvider.getTokenType(accessToken);
        assertThat(tokenType).isNull();
    }

    @Test
    void getAccessExpirationShouldReturnConfiguredValue() {
        assertThat(jwtTokenProvider.getAccessExpiration()).isEqualTo(ACCESS_EXPIRATION);
    }

    @Test
    void getRefreshExpirationShouldReturnConfiguredValue() {
        assertThat(jwtTokenProvider.getRefreshExpiration()).isEqualTo(REFRESH_EXPIRATION);
    }
}
