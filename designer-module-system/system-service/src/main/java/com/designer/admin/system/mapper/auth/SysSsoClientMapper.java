package com.designer.admin.system.mapper.auth;

import com.designer.admin.system.model.entity.auth.SysSsoClient;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import org.apache.ibatis.annotations.Mapper;

/**
 * SSO 客户端数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysSsoClientMapper extends MPJBaseMapper<SysSsoClient> {

    /**
     * 统计客户端标识占用数（只统计未删除的记录）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的标识不再占位。</p>
     *
     * @param clientKey 客户端标识
     * @param excludeId 需要排除的客户端 ID，新增时传 {@code null}
     * @return 占用该标识的记录数
     */
    default Long countByClientKey(String clientKey, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysSsoClient.class)
                .eq(SysSsoClient::getClientKey, clientKey)
                .ne(excludeId != null, SysSsoClient::getId, excludeId));
    }

    /**
     * 统计回跳来源占用数（只统计未删除的记录）。
     *
     * <p>唯一性由应用层保证（表上无唯一索引），逻辑删除的来源不再占位。</p>
     *
     * @param origin    回跳来源（协议 + 域名 + 端口）
     * @param excludeId 需要排除的客户端 ID，新增时传 {@code null}
     * @return 占用该来源的记录数
     */
    default Long countByOrigin(String origin, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysSsoClient.class)
                .eq(SysSsoClient::getOrigin, origin)
                .ne(excludeId != null, SysSsoClient::getId, excludeId));
    }
}
