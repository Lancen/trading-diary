package com.tradingdiary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 刷新令牌，存储 JWT refresh token 用于令牌续期
 */
@Getter
@Setter
@TableName("sys_refresh_token")
public class SysRefreshToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 令牌哈希值 */
    private String tokenHash;

    /** 令牌过期时间 */
    private LocalDateTime expiresAt;

    /** 是否已撤销 */
    private Boolean revoked = false;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
