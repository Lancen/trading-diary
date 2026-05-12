package com.tradingdiary.service;

import com.tradingdiary.entity.SysRefreshToken;
import com.tradingdiary.entity.SysRole;
import com.tradingdiary.entity.SysUser;
import com.tradingdiary.exception.UnauthorizedException;
import com.tradingdiary.mapper.SysRefreshTokenMapper;
import com.tradingdiary.mapper.SysRoleMapper;
import com.tradingdiary.mapper.SysUserMapper;
import com.tradingdiary.model.request.LoginRequest;
import com.tradingdiary.model.vo.TokenVO;
import com.tradingdiary.model.vo.UserInfoVO;
import com.tradingdiary.security.JwtTokenProvider;
import com.tradingdiary.security.SecurityExceptionHandler;
import com.tradingdiary.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRefreshTokenMapper sysRefreshTokenMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityExceptionHandler securityExceptionHandler;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        SysUser sysUser = new SysUser();
        sysUser.setId(1L);
        sysUser.setUsername("admin");

        SysRole role = new SysRole();
        role.setCode("ADMIN");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        when(sysUserMapper.selectByUsername("admin")).thenReturn(sysUser);
        when(sysRoleMapper.selectByUserId(1L)).thenReturn(List.of(role));
        when(jwtTokenProvider.issueAccessToken(1L, List.of("ADMIN"))).thenReturn("access-token-value");
        when(jwtTokenProvider.issueRefreshToken(1L)).thenReturn("refresh-token-value");
        when(jwtTokenProvider.getAccessExpiration()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604800L);

        TokenVO result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token-value");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(result.getExpiresIn()).isEqualTo(900L);

        verify(sysRefreshTokenMapper).insert(any(SysRefreshToken.class));
    }

    @Test
    void shouldThrowUnauthorizedExceptionForWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        BadCredentialsException badCredentials = new BadCredentialsException("Bad credentials");
        UnauthorizedException translated = new UnauthorizedException(100101, "用户名或密码错误");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(badCredentials);
        when(securityExceptionHandler.translate(badCredentials)).thenReturn(translated);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("用户名或密码错误");

        verify(sysUserMapper, never()).selectByUsername(anyString());
        verify(jwtTokenProvider, never()).issueAccessToken(anyLong(), any());
    }

    @Test
    void shouldThrowUnauthorizedExceptionForDisabledAccount() {
        LoginRequest request = new LoginRequest();
        request.setUsername("disableduser");
        request.setPassword("password");

        DisabledException disabledException = new DisabledException("User is disabled");
        UnauthorizedException translated = new UnauthorizedException(100102, "账户已被禁用");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(disabledException);
        when(securityExceptionHandler.translate(disabledException)).thenReturn(translated);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("账户已被禁用");
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        String oldRefreshToken = "old-refresh-token-value";

        SysRole role = new SysRole();
        role.setCode("USER");

        SysRefreshToken stored = new SysRefreshToken();
        stored.setId(10L);
        stored.setUserId(1L);
        stored.setRevoked(false);
        stored.setExpiresAt(LocalDateTime.now().plusDays(7));

        // Hash of oldRefreshToken: the service hashes it internally with SHA-256
        // We need to compute the hash to set up the mock correctly
        String tokenHash = sha256(oldRefreshToken);
        stored.setTokenHash(tokenHash);

        when(jwtTokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(oldRefreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserId(oldRefreshToken)).thenReturn(1L);
        when(sysRefreshTokenMapper.selectValidByUserId(1L)).thenReturn(stored);
        when(sysRoleMapper.selectByUserId(1L)).thenReturn(List.of(role));
        when(jwtTokenProvider.issueAccessToken(1L, List.of("USER"))).thenReturn("new-access-token");
        when(jwtTokenProvider.issueRefreshToken(1L)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessExpiration()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604800L);

        TokenVO result = authService.refresh(oldRefreshToken);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

        // Old token should be revoked
        verify(sysRefreshTokenMapper).updateById(stored);
        assertThat(stored.getRevoked()).isTrue();

        // New token should be stored
        verify(sysRefreshTokenMapper).insert(any(SysRefreshToken.class));
    }

    @Test
    void shouldThrowUnauthorizedExceptionForInvalidRefreshToken() {
        String invalidToken = "invalid-token";

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(invalidToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("无效或已过期");
    }

    @Test
    void shouldLogoutSuccessfully() {
        authService.logout(1L);

        verify(sysRefreshTokenMapper).revokeByUserId(1L);
    }

    @Test
    void shouldGetCurrentUser() {
        SysUser sysUser = new SysUser();
        sysUser.setId(1L);
        sysUser.setUsername("testuser");
        sysUser.setNickname("Test User");

        SysRole role = new SysRole();
        role.setCode("USER");

        when(sysUserMapper.selectById(1L)).thenReturn(sysUser);
        when(sysRoleMapper.selectByUserId(1L)).thenReturn(List.of(role));

        UserInfoVO result = authService.getCurrentUser(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getNickname()).isEqualTo("Test User");
        assertThat(result.getRoles()).containsExactly("USER");
    }

    @Test
    void shouldThrowUnauthorizedExceptionForNonexistentUser() {
        when(sysUserMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> authService.getCurrentUser(999L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void loginWithNoRolesShouldStillSucceed() {
        LoginRequest request = new LoginRequest();
        request.setUsername("noroleuser");
        request.setPassword("password");

        SysUser sysUser = new SysUser();
        sysUser.setId(5L);
        sysUser.setUsername("noroleuser");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("noroleuser");
        when(sysUserMapper.selectByUsername("noroleuser")).thenReturn(sysUser);
        when(sysRoleMapper.selectByUserId(5L)).thenReturn(Collections.emptyList());
        when(jwtTokenProvider.issueAccessToken(5L, Collections.emptyList())).thenReturn("access-token");
        when(jwtTokenProvider.issueRefreshToken(5L)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessExpiration()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604800L);

        TokenVO result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
    }

    // Helper: compute SHA-256 hash to match AuthServiceImpl's internal hashing
    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
