package com.designer.admin.system.mapper.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.system.model.entity.auth.SysOauthClientScope;
import com.designer.admin.system.model.entity.auth.SysOauthScope;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OAuth2 客户端与授权范围的关联数据访问。
 *
 * <p>关联查询用 mpj Wrapper 表达，不再写 {@code @Select} 拼 SQL：
 * 主表是关联表，Scope 表通过 innerJoin 关联，其 {@code @TableLogic} 逻辑删除条件由
 * mpj 自动追加（ {@code ConfigProperties.subTableLogic} 默认开启），无需手写 {@code deleted = 0}。</p>
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysOauthClientScopeMapper extends MPJBaseMapper<SysOauthClientScope> {

    /**
     * 查询客户端已授权的范围编码。
     *
     * @param clientPk 客户端主键
     * @return 范围编码列表
     */
    default List<String> selectScopeCodesByClientPk(Long clientPk) {
        return selectJoinList(String.class, JoinWrappers.lambda(SysOauthClientScope.class)
                .select(SysOauthScope::getScopeCode)
                .innerJoin(SysOauthScope.class, SysOauthScope::getId, SysOauthClientScope::getScopeId)
                .eq(SysOauthClientScope::getClientId, clientPk)
                .eq(SysOauthScope::getStatus, SystemConsts.STATUS_ENABLE)
                .orderByAsc(SysOauthScope::getSort, SysOauthScope::getId));
    }

    /**
     * 查询客户端已授权的范围 ID。
     *
     * <p>单表查询，不需要 join。</p>
     *
     * @param clientPk 客户端主键
     * @return 范围 ID 列表
     */
    default List<Long> selectScopeIdsByClientPk(Long clientPk) {
        return selectList(Wrappers.<SysOauthClientScope>lambdaQuery()
                .select(SysOauthClientScope::getScopeId)
                .eq(SysOauthClientScope::getClientId, clientPk))
                .stream()
                .map(SysOauthClientScope::getScopeId)
                .toList();
    }
}
