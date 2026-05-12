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
 * Dev-mode auto-login filter.
 * <p>
 * Only active when {@code spring.profiles.active=dev}.
 * Automatically authenticates as the configured username so developers
 * don't need to manually log in during local development.
 * <p>
 * Executes BEFORE {@link JwtAuthFilter} in the security filter chain.
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
