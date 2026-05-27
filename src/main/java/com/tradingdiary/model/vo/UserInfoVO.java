package com.tradingdiary.model.vo;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户信息视图对象，包含用户基本信息和角色列表
 */
@Getter
@Setter
public class UserInfoVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色列表 */
    private List<String> roles;
}
