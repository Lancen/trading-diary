package com.tradingdiary.service;

import com.tradingdiary.model.request.LoginRequest;
import com.tradingdiary.model.vo.TokenVO;
import com.tradingdiary.model.vo.UserInfoVO;

public interface AuthService {

    TokenVO login(LoginRequest request);

    TokenVO refresh(String refreshToken);

    void logout(Long userId);

    UserInfoVO getCurrentUser(Long userId);
}
