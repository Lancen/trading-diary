package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SysRefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysRefreshTokenMapper extends BaseMapper<SysRefreshToken> {

    @Select("SELECT * FROM sys_refresh_token " +
            "WHERE user_id = #{userId} AND revoked = 0 AND expires_at > NOW() " +
            "ORDER BY created_at DESC LIMIT 1")
    SysRefreshToken selectValidByUserId(@Param("userId") Long userId);

    @Update("UPDATE sys_refresh_token SET revoked = 1 " +
            "WHERE user_id = #{userId} AND revoked = 0")
    int revokeByUserId(@Param("userId") Long userId);
}
