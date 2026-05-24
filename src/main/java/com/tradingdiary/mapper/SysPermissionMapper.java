package com.tradingdiary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tradingdiary.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统权限 Mapper，提供权限数据的基础 CRUD 操作
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
}
