package com.tradingdiary.service;

import com.tradingdiary.model.request.LoginRequest;
import com.tradingdiary.model.vo.TokenVO;
import com.tradingdiary.model.vo.UserInfoVO;

/**
 * 认证服务，封装用户登录、令牌刷新与登出逻辑
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录成功后返回的令牌信息
     */
    TokenVO login(LoginRequest request);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的令牌信息
     */
    TokenVO refresh(String refreshToken);

    /**
     * 用户登出
     *
     * @param userId 用户ID
     */
    void logout(Long userId);

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoVO getCurrentUser(Long userId);

    /**
     * 根据用户名查询用户ID
     *
     * @param username 用户名
     * @return 用户ID，若用户不存在则返回 null
     */
    Long getUserIdByUsername(String username);
}
