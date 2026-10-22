package com.designer.admin.system.constant;

/**
 * OAuth2 协议常量（RFC 6749）：授权类型、请求/响应键名、错误码与响应状态码。
 *
 * <p>令牌端点是面向第三方客户端的协议接口，键名一律是下划线风格，
 * 与项目内驼峰命名的 DTO/VO 规范不同，因此由 VO 上的 {@code @ONodeAttr(name = ...)}
 * 引用这里的常量完成映射，避免把协议键名散落成字面量。</p>
 *
 * @author 雨夜终端
 */
public final class Oauth2Consts {

    /** 授权类型：客户端凭证（Client Credentials） */
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    /** 令牌类型：Bearer */
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    /** 客户端未配置有效期时的默认值（秒）：2 小时 */
    public static final int DEFAULT_ACCESS_TOKEN_TTL_SECONDS = 7200;

    /** access_token 有效期下限（秒） */
    public static final int MIN_ACCESS_TOKEN_TTL_SECONDS = 60;

    /** clientId 允许的字符与长度：小写字母开头，3-64 位字母数字、下划线、短横线 */
    public static final String CLIENT_ID_PATTERN = "^[a-z][a-z0-9_-]{2,63}$";

    /** 客户端密钥随机字节数，转十六进制后为 48 位 */
    public static final int CLIENT_SECRET_BYTE_LENGTH = 24;

    /** RFC 6749 请求参数：授权类型 */
    public static final String PARAM_GRANT_TYPE = "grant_type";

    /** RFC 6749 请求参数：客户端 ID */
    public static final String PARAM_CLIENT_ID = "client_id";

    /** RFC 6749 请求参数：客户端密钥 */
    public static final String PARAM_CLIENT_SECRET = "client_secret";

    /** RFC 6749 请求参数：授权范围 */
    public static final String PARAM_SCOPE = "scope";

    /** 兼容参数：授权类型（驼峰写法，历史调用方） */
    public static final String PARAM_GRANT_TYPE_CAMEL = "grantType";

    /** 兼容参数：客户端 ID（驼峰写法，历史调用方） */
    public static final String PARAM_CLIENT_ID_CAMEL = "clientId";

    /** 兼容参数：客户端密钥（驼峰写法，历史调用方） */
    public static final String PARAM_CLIENT_SECRET_CAMEL = "clientSecret";

    /** RFC 6749 响应字段：访问令牌 */
    public static final String FIELD_ACCESS_TOKEN = "access_token";

    /** RFC 6749 响应字段：令牌类型 */
    public static final String FIELD_TOKEN_TYPE = "token_type";

    /** RFC 6749 响应字段：有效期（秒） */
    public static final String FIELD_EXPIRES_IN = "expires_in";

    /** RFC 6749 响应字段：授权范围 */
    public static final String FIELD_SCOPE = "scope";

    /** RFC 6749 错误响应字段：错误码 */
    public static final String FIELD_ERROR = "error";

    /** RFC 6749 错误响应字段：错误描述 */
    public static final String FIELD_ERROR_DESCRIPTION = "error_description";

    /** OAuth2 错误码：授权类型不支持 */
    public static final String ERROR_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    /** OAuth2 错误码：客户端认证失败 */
    public static final String ERROR_INVALID_CLIENT = "invalid_client";

    /** OAuth2 错误码：授权范围非法 */
    public static final String ERROR_INVALID_SCOPE = "invalid_scope";

    /** OAuth2 错误码：请求参数非法 */
    public static final String ERROR_INVALID_REQUEST = "invalid_request";

    /** 承载客户端凭证的请求头名 */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Basic 认证前缀 */
    public static final String BASIC_PREFIX = "Basic ";

    /** 客户端 id 与密钥的分隔符 */
    public static final char COLON = ':';

    /** 成功状态码 */
    public static final int HTTP_OK = 200;

    /** 参数错误状态码 */
    public static final int HTTP_BAD_REQUEST = 400;

    private Oauth2Consts() {
    }
}
