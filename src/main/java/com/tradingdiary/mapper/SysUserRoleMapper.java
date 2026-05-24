package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户-角色关联 Mapper，提供用户与角色映射关系的基础 CRUD 操作
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
