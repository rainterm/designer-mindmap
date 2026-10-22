package com.designer.admin.system.service.auth;

import com.designer.admin.system.model.vo.auth.Oauth2TokenVO;

/**
 * OAuth2 Client Credentials 换票
 *
 * @author 雨夜终端
 */
public interface Oauth2TokenService {

    /**
     * Client Credentials 模式换票：校验客户端密钥与授权范围后签发 access_token。
     *
     * <p>返回值是 RFC 6749 令牌响应：字段在 Java 侧保持驼峰命名，
     * 通过 {@code @ONodeAttr(name = ...)} 映射为协议要求的下划线键名。</p>
     *
     * @param clientId     客户端 ID
     * @param clientSecret 客户端密钥（明文，内部 BCrypt 比对）
     * @param scope        申请的 Scope，空格分隔；为空时取客户端全部授权 Scope
     * @return 令牌响应（access_token、token_type、expires_in、scope）
     */
    Oauth2TokenVO issueClientCredentials(String clientId, String clientSecret, String scope);
}
