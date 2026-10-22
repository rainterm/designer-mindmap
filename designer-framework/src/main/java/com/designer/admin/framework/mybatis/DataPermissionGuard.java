package com.designer.admin.framework.mybatis;

import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.PermissionLoader;
import com.designer.admin.framework.satoken.PermissionSnapshot;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.List;

/**
 * 数据权限的「点查」判定：某个部门 / 某条归属数据是否在当前用户范围内。
 *
 * <p><b>为什么不只依赖 SQL 自动改写</b>：自动改写覆盖的是「已经有 WHERE 的读写」，
 * 但有一类业务规则必须在<b>写入前</b>判断，否则会写出一条自己看不见的数据：
 * 把用户调到别的部门、把数据挂到别的部门、给角色授予自己范围外的部门。
 * SQL 改写对 {@code INSERT} 不生效（没有 WHERE 可加），这类校验只能显式做。</p>
 *
 * <p>除此之外的业务查询<b>不要</b>调本类 —— 那些查询由拦截器统一过滤，
 * 重复判断只会让「谁是数据权限的唯一入口」变得模糊。</p>
 *
 * @author 雨夜终端
 */
@Component
public class DataPermissionGuard {

    @Inject
    private PermissionLoader permissionLoader;

    /**
     * 当前用户是否不受数据权限限制（超管 / 全部数据 / 无登录态）。
     *
     * @return 不受限返回 {@code true}
     */
    public boolean isUnrestricted() {
        PermissionSnapshot snapshot = currentSnapshot();
        return snapshot == null || snapshot.dataScopeUnrestricted();
    }

    /**
     * 当前用户能否看到某个部门。
     *
     * <p>「仅本人」档在部门树上退化为「本人所在部门」，与 SQL 改写的口径保持一致。</p>
     *
     * @param deptId 部门 ID
     * @return 可见返回 {@code true}
     */
    public boolean canSeeDept(Long deptId) {
        if (deptId == null) {
            return true;
        }
        PermissionSnapshot snapshot = currentSnapshot();
        if (snapshot == null || snapshot.dataScopeUnrestricted()) {
            return true;
        }
        List<Long> deptIds = snapshot.getDataScopeDeptIds();
        if (deptIds != null && deptIds.contains(deptId)) {
            return true;
        }
        return snapshot.dataScopeHasSelf() && deptId.equals(snapshot.getDataScopeDeptId());
    }

    /**
     * 当前用户的权限快照；无登录态返回 {@code null}（视为不受限）。
     */
    private PermissionSnapshot currentSnapshot() {
        Long userId = LoginHelper.getUserIdOrNull();
        if (userId == null) {
            return null;
        }
        return permissionLoader.load(userId);
    }
}
