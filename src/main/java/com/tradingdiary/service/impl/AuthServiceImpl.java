package com.tradingdiary.service.impl;

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
import com.tradingdiary.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysRefreshTokenMapper sysRefreshTokenMapper;

    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final SecurityExceptionHandler securityExceptionHandler;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           SysRoleMapper sysRoleMapper,
                           SysRefreshTokenMapper sysRefreshTokenMapper,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           SecurityExceptionHandler securityExceptionHandler) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRefreshTokenMapper = sysRefreshTokenMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Override
    public TokenVO login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            String username = authentication.getName();
            SysUser sysUser = sysUserMapper.selectByUsername(username);
            Long userId = sysUser.getId();

            List<SysRole> roles = sysRoleMapper.selectByUserId(userId);
            List<String> roleCodes = roles.stream()
                    .map(SysRole::getCode)
                    .collect(Collectors.toList());

            String accessToken = jwtTokenProvider.issueAccessToken(userId, roleCodes);
            String refreshToken = jwtTokenProvider.issueRefreshToken(userId);

            storeRefreshToken(userId, refreshToken);

            TokenVO tokenVO = new TokenVO();
            tokenVO.setAccessToken(accessToken);
            tokenVO.setRefreshToken(refreshToken);
            tokenVO.setExpiresIn(jwtTokenProvider.getAccessExpiration());

            return tokenVO;

        } catch (AuthenticationException e) {
            throw securityExceptionHandler.translate(e);
        }
    }

    @Override
    public TokenVO refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token 无效或已过期");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new UnauthorizedException("Token 类型错误，需要 refresh token");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String tokenHash = hashToken(refreshToken);

        SysRefreshToken stored = sysRefreshTokenMapper.selectValidByUserId(userId);
        if (stored == null
                || Boolean.TRUE.equals(stored.getRevoked())
                || stored.getExpiresAt().isBefore(LocalDateTime.now())
                || !tokenHash.equals(stored.getTokenHash())) {
            throw new UnauthorizedException("Refresh token 无效或已被撤销");
        }

        stored.setRevoked(true);
        sysRefreshTokenMapper.updateById(stored);

        List<SysRole> roles = sysRoleMapper.selectByUserId(userId);
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());

        String newAccessToken = jwtTokenProvider.issueAccessToken(userId, roleCodes);
        String newRefreshToken = jwtTokenProvider.issueRefreshToken(userId);

        storeRefreshToken(userId, newRefreshToken);

        TokenVO tokenVO = new TokenVO();
        tokenVO.setAccessToken(newAccessToken);
        tokenVO.setRefreshToken(newRefreshToken);
        tokenVO.setExpiresIn(jwtTokenProvider.getAccessExpiration());

        return tokenVO;
    }

    @Override
    public void logout(Long userId) {
        sysRefreshTokenMapper.revokeByUserId(userId);
    }

    @Override
    public UserInfoVO getCurrentUser(Long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null || Boolean.TRUE.equals(sysUser.getIsDeleted())) {
            throw new UnauthorizedException("用户不存在");
        }

        List<SysRole> roles = sysRoleMapper.selectByUserId(userId);
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());

        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(sysUser.getId());
        userInfoVO.setUsername(sysUser.getUsername());
        userInfoVO.setNickname(sysUser.getNickname());
        userInfoVO.setRoles(roleCodes);

        return userInfoVO;
    }

    @Override
    public Long getUserIdByUsername(String username) {
        SysUser sysUser = sysUserMapper.selectByUsername(username);
        return sysUser != null ? sysUser.getId() : null;
    }

    private void storeRefreshToken(Long userId, String rawToken) {
        SysRefreshToken token = new SysRefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshExpiration()));
        token.setRevoked(false);
        sysRefreshTokenMapper.insert(token);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
