package com.designer.admin.system.mapper.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.rbac.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
