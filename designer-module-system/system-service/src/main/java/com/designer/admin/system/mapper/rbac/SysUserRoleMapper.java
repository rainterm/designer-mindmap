package com.designer.admin.system.mapper.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
