package com.tradingdiary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingdiary.model.request.LoginRequest;
import com.tradingdiary.model.vo.TokenVO;
import com.tradingdiary.model.vo.UserInfoVO;
import com.tradingdiary.security.JwtAuthFilter;
import com.tradingdiary.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── T059: Login Tests ────────────────────────────────────────────

    @Test
    void shouldReturnTokenOnValidLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        TokenVO tokenVO = new TokenVO();
        tokenVO.setAccessToken("access-token-value");
        tokenVO.setRefreshToken("refresh-token-value");
        tokenVO.setExpiresIn(900L);

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenVO);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-value"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void shouldReturn400ForEmptyUsername() throws Exception {
        String body = "{\"username\": \"\", \"password\": \"password123\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400001));
    }

    @Test
    void shouldReturn400ForBlankPassword() throws Exception {
        String body = "{\"username\": \"test\", \"password\": \"   \"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400001));
    }

    // ─── T059: /me Tests ──────────────────────────────────────────────

    @Test
    void shouldReturnUserInfoForAuthenticatedUser() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(authService.getUserIdByUsername("testuser")).thenReturn(1L);

        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(1L);
        userInfoVO.setUsername("testuser");
        userInfoVO.setNickname("Test User");
        userInfoVO.setRoles(List.of("USER"));

        when(authService.getCurrentUser(1L)).thenReturn(userInfoVO);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.nickname").value("Test User"));
    }

    @Test
    void shouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(100101));
    }

    // ─── T062: Logout Tests ──────────────────────────────────────────

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "logoutuser", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(authService.getUserIdByUsername("logoutuser")).thenReturn(42L);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登出成功"));

        verify(authService).logout(42L);
    }

    @Test
    void logoutWithoutAuthenticationShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(100101));
    }
}
