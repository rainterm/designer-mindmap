package com.designer.admin.system.model.entity.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OAuth2 客户端与 Scope 关联
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_oauth_client_scope")
public class SysOauthClientScope {

    /** OAuth2 客户端ID */
    private Long clientId;

    /** 授权范围ID */
    private Long scopeId;
}
