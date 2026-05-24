package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SysRefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统刷新令牌 Mapper，提供刷新令牌的查询与撤销操作
 */
@Mapper
public interface SysRefreshTokenMapper extends BaseMapper<SysRefreshToken> {

    /**
     * 查询指定用户的有效（未撤销且未过期）刷新令牌，取最近一条
     *
     * @param userId 用户ID
     * @return 有效的刷新令牌，不存在时返回 null
     */
    SysRefreshToken selectValidByUserId(@Param("userId") Long userId);

    /**
     * 撤销指定用户的所有未撤销刷新令牌
     *
     * @param userId 用户ID
     * @return 受影响的行数
     */
    int revokeByUserId(@Param("userId") Long userId);
}
