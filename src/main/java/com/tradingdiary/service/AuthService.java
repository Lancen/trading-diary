package com.tradingdiary.service;

import com.tradingdiary.model.request.LoginRequest;
import com.tradingdiary.model.vo.TokenVO;
import com.tradingdiary.model.vo.UserInfoVO;

public interface AuthService {

    TokenVO login(LoginRequest request);

    TokenVO refresh(String refreshToken);

    void logout(Long userId);

    UserInfoVO getCurrentUser(Long userId);

    /**
     * 根据用户名查询用户ID
     *
     * @param username 用户名
     * @return 用户ID，若用户不存在则返回 null
     */
    Long getUserIdByUsername(String username);
}
