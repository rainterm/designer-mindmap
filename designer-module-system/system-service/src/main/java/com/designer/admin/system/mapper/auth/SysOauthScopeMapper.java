package com.designer.admin.system.mapper.auth;

import com.designer.admin.system.model.entity.auth.SysOauthScope;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2 授权范围数据访问。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysOauthScopeMapper extends MPJBaseMapper<SysOauthScope> {

    /**
     * 统计 Scope 标识占用数（只统计未删除的记录）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的标识不再占位。</p>
     *
     * @param scopeCode Scope 标识
     * @param excludeId 需要排除的 Scope ID，新增时传 {@code null}
     * @return 占用该标识的记录数
     */
    default Long countByScopeCode(String scopeCode, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysOauthScope.class)
                .eq(SysOauthScope::getScopeCode, scopeCode)
                .ne(excludeId != null, SysOauthScope::getId, excludeId));
    }
}
