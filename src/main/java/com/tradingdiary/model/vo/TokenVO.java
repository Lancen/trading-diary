package com.tradingdiary.model.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 令牌视图对象，包含访问令牌和刷新令牌
 */
@Getter
@Setter
public class TokenVO {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 过期时间（秒） */
    private Long expiresIn;
}
