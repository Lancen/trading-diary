package com.tradingdiary.security;

import com.tradingdiary.exception.UnauthorizedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class SecurityExceptionHandler {

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
        return new UnauthorizedException(100100, "认证失败: " + ex.getMessage());
    }
}
