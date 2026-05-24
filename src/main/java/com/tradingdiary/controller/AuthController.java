package com.tradingdiary.controller;

import com.tradingdiary.exception.UnauthorizedException;
import com.tradingdiary.model.ApiResponse;
import com.tradingdiary.model.request.LoginRequest;
import com.tradingdiary.model.vo.TokenVO;
import com.tradingdiary.model.vo.UserInfoVO;
import com.tradingdiary.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器，处理用户登录、令牌刷新、登出和当前用户信息查询
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenVO> login(@RequestBody @Valid LoginRequest request) {
        try {
            TokenVO tokenVO = authService.login(request);
            return ApiResponse.ok(tokenVO);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("refreshToken 不能为空");
        }
        try {
            TokenVO tokenVO = authService.refresh(refreshToken);
            return ApiResponse.ok(tokenVO);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Token 刷新失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Long userId = getCurrentUserId();
        authService.logout(userId);
        return ApiResponse.ok(null, "登出成功");
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> me() {
        Long userId = getCurrentUserId();
        UserInfoVO userInfoVO = authService.getCurrentUser(userId);
        return ApiResponse.ok(userInfoVO);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("用户未登录");
        }
        String username = authentication.getName();
        Long userId = authService.getUserIdByUsername(username);
        if (userId == null) {
            throw new UnauthorizedException("用户不存在");
        }
        return userId;
    }
}
