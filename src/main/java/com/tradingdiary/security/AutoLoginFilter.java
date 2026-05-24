package com.tradingdiary.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 开发模式自动登录过滤器。
 * <p>
 * 仅在 {@code spring.profiles.active=dev} 时激活。
 * 自动以配置的用户名进行认证，开发者在本地开发时无需手动登录。
 * <p>
 * 在安全过滤器链中位于 {@link JwtAuthFilter} 之前执行。
 */
@Component
@Profile("dev")
public class AutoLoginFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AutoLoginFilter.class);

    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.auto-login.username:admin}")
    private String autoLoginUsername;

    public AutoLoginFilter(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * 包内可见的 setter，用于不依赖 Spring 上下文的测试。
     */
    void setUsername(String username) {
        this.autoLoginUsername = username;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(autoLoginUsername);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Dev auto-login as '{}'", autoLoginUsername);
        } catch (UsernameNotFoundException e) {
            log.warn("Dev auto-login user '{}' not found — filter skipped", autoLoginUsername);
        }

        filterChain.doFilter(request, response);
    }
}
