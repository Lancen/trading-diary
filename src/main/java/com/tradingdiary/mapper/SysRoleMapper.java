package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色 Mapper，提供角色数据的查询
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询其关联的所有角色
     *
     * @param userId 用户ID
     * @return 该用户关联的角色列表
     */
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
