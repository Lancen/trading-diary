package com.tradingdiary.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenVO {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
