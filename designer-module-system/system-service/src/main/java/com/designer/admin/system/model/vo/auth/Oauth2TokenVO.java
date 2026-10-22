package com.designer.admin.system.model.vo.auth;

import com.designer.admin.system.constant.Oauth2Consts;
import lombok.Builder;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * OAuth2 访问令牌响应（RFC 6749 §5.1）。
 *
 * <p>字段名通过 {@link ONodeAttr#name()} 映射为协议规定的下划线键名，
 * 既保留 Java 侧的驼峰命名，又不必为了报文格式使用 {@code Map}。
 * 键名取值来自 {@link Oauth2Consts}，与错误响应共用同一份定义。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class Oauth2TokenVO {

    /** 访问令牌 */
    @ONodeAttr(name = Oauth2Consts.FIELD_ACCESS_TOKEN)
    private String accessToken;

    /** 令牌类型，固定 Bearer */
    @ONodeAttr(name = Oauth2Consts.FIELD_TOKEN_TYPE)
    private String tokenType;

    /** 有效期（秒） */
    @ONodeAttr(name = Oauth2Consts.FIELD_EXPIRES_IN)
    private Long expiresIn;

    /** 授予的 scope，空格分隔 */
    @ONodeAttr(name = Oauth2Consts.FIELD_SCOPE)
    private String scope;
}
