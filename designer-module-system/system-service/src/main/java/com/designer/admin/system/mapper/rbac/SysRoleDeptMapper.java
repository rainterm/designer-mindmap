package com.designer.admin.system.mapper.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.rbac.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据权限（自定义部门）关联数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {
}
