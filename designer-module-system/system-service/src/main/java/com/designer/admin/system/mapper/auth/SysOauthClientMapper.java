package com.designer.admin.system.mapper.auth;

import com.designer.admin.system.model.entity.auth.SysOauthClient;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2 客户端数据访问。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysOauthClientMapper extends MPJBaseMapper<SysOauthClient> {

    /**
     * 统计 client_id 占用数（只统计未删除的记录）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的 client_id 不再占位。</p>
     *
     * @param clientId  OAuth2 客户端标识
     * @param excludeId 需要排除的客户端 ID，新增时传 {@code null}
     * @return 占用该标识的记录数
     */
    default Long countByClientId(String clientId, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysOauthClient.class)
                .eq(SysOauthClient::getClientId, clientId)
                .ne(excludeId != null, SysOauthClient::getId, excludeId));
    }
}
