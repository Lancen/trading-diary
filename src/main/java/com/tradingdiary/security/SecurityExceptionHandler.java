package com.tradingdiary.security;

import com.tradingdiary.exception.UnauthorizedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * 安全异常处理器，将 Spring Security 异常转为统一 API 响应
 */
@Component
public class SecurityExceptionHandler {

    /**
     * 将 Spring Security 认证异常翻译为业务异常
     *
     * @param ex Spring Security 认证异常
     * @return 对应的 UnauthorizedException
     */
    public UnauthorizedException translate(AuthenticationException ex) {
        if (ex instanceof BadCredentialsException) {
            return new UnauthorizedException(100101, "用户名或密码错误");
        }
        if (ex instanceof DisabledException) {
            return new UnauthorizedException(100102, "账户已被禁用");
        }
        if (ex instanceof LockedException) {
            return new UnauthorizedException(100103, "账户已被锁定");
        }
        return new UnauthorizedException(100100, "认证失败");
    }
}
