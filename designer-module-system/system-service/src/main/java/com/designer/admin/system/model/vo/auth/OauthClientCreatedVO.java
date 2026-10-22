package com.designer.admin.system.model.vo.auth;

import lombok.Data;

/**
 * 创建/重置密钥后仅返回一次明文 secret
 *
 * @author 雨夜终端
 */
@Data
public class OauthClientCreatedVO {

    /** 客户端ID（sys_oauth_client 主键） */
    private Long id;

    /** 客户端标识 */
    private String clientId;

    /** 明文密钥：仅在创建/重置时返回这一次，之后库里只存哈希 */
    private String clientSecret;

    /** 客户端名称 */
    private String clientName;
}
