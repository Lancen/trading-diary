package com.tradingdiary.model.vo;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private List<String> roles;
}
