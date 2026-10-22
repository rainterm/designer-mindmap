package com.designer.admin.system.mapper.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.framework.mybatis.DataPermissionContext;
import com.designer.admin.system.model.entity.rbac.SysRole;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.entity.rbac.SysUserRole;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * 系统用户数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysUserMapper extends MPJBaseMapper<SysUser> {

    /**
     * 统计登录账号占用数（只统计未删除的用户）。
     *
     * <p>账号唯一性完全在应用层判断：表上没有唯一索引，所以逻辑删除的用户不再占着账号，
     * 删除后可以用同一个账号重新建号。这里的查询必须走 {@code @TableLogic} 默认条件
     * （即只看 {@code deleted = 0}），否则已删除的账号会永远无法复用。</p>
     *
     * @param username  登录账号
     * @param excludeId 需要排除的用户 ID，新增时传 {@code null}
     * @return 占用该账号的用户数
     */
    default Long countByUsername(String username, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysUser.class)
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
    }

    /**
     * 查询用户的所属部门 ID（只取一列，供数据权限装载使用）。
     *
     * @param userId 用户 ID
     * @return 部门 ID；用户不存在或未分配部门时返回 {@code null}
     */
    default Long selectDeptIdById(Long userId) {
        // 装载权限快照时不能反过来被数据权限过滤（否则自定义范围里没有本部门时，deptId 会被裁成 null）
        return DataPermissionContext.ignore(() -> {
            SysUser user = selectOne(new LambdaQueryWrapper<SysUser>()
                    .select(SysUser::getDeptId)
                    .eq(SysUser::getId, userId));
            return user == null ? null : user.getDeptId();
        });
    }

    /**
     * 判断这批用户中是否存在超级管理员（以 {@code sys_role.is_super} 为准）。
     *
     * <p><b>为什么不再判断 {@code id == 1}</b>：超管身份的唯一事实来源是角色的
     * {@code is_super} 标记（与 {@code StpInterfaceImpl} 鉴权口径一致）。硬编码主键
     * 在换库、导入数据、新增超管后都会失效 —— 删号/停用保护形同虚设。</p>
     *
     * <p><b>刻意不校验角色状态</b>：鉴权时「停用的角色不算超管」，但这里是删除/停用的
     * <b>保护性</b>判定，不可逆操作宁可多拦一次。角色被临时停用并不代表这个账号
     * 可以当普通用户删掉——重建成本远高于一次误报。</p>
     *
     * @param userIds 待校验的用户 ID，允许为空
     * @return 存在任一超管返回 {@code true}
     */
    default boolean existsSuperUser(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        // 必须绕开数据权限：这是「保护性」判定，被过滤掉的超管会让删除/停用保护静默失效。
        // 表里只有 protected 关键字，另外 sys_user 本身有 dept_id，被裁掉就查不到
        return DataPermissionContext.ignore(() -> {
            Long cnt = selectJoinCount(JoinWrappers.lambda(SysUser.class)
                    .leftJoin(SysUserRole.class, SysUserRole::getUserId, SysUser::getId)
                    .leftJoin(SysRole.class, SysRole::getId, SysUserRole::getRoleId)
                    .in(SysUser::getId, userIds)
                    .eq(SysRole::getIsSuper, SystemConsts.IS_SUPER_YES));
            return cnt != null && cnt > 0;
        });
    }
}
