package com.designer.admin.system.controller.open;

import cn.dev33.satoken.annotation.SaIgnore;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.system.constant.Oauth2Consts;
import com.designer.admin.system.model.vo.auth.Oauth2ErrorVO;
import com.designer.admin.system.model.vo.auth.Oauth2TokenVO;
import com.designer.admin.system.service.auth.Oauth2TokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.util.MimeType;

import java.util.Base64;

/**
 * OAuth2 令牌端点（Client Credentials）。
 *
 * <p>该端点是面向第三方客户端的协议接口，请求与响应都必须使用 RFC 6749 规定的下划线键名。
 * 唯一对外路径为 {@code /open-api/oauth2/token}：后端 8080 不对外开放，统一经 nginx 转发。</p>
 * <p>响应体使用 {@link Oauth2TokenVO} / {@link Oauth2ErrorVO} 承载，
 * 由 {@code @ONodeAttr(name = ...)} 完成驼峰属性到协议键名的映射，键名统一取自 {@link Oauth2Consts}。
 * 成功与失败都直接返回 RFC6749 JSON，不包 Result 外壳。</p>
 *
 * @author 雨夜终端
 */
@Controller
@Mapping("/open-api/oauth2")
@Api(tags = "OAuth2 令牌")
public class Oauth2TokenController {

    @Inject
    private Oauth2TokenService tokenService;

    @SaIgnore
    @Post
    @Mapping("/token")
    @ApiOperation(value = "获取 OAuth2 访问令牌",
            notes = "对外统一经 nginx 的 /open-api 前缀访问（后端 8080 不对外开放）。"
                    + "Content-Type 为 application/x-www-form-urlencoded；"
                    + "参数：grant_type=client_credentials、client_id、client_secret、scope（空格分隔）；"
                    + "client_id 与 client_secret 也可由 Authorization: Basic 请求头携带")
    public void token(Context ctx) {
        String grantType = firstNonBlank(ctx.param(Oauth2Consts.PARAM_GRANT_TYPE),
                ctx.param(Oauth2Consts.PARAM_GRANT_TYPE_CAMEL));
        if (grantType == null || !Oauth2Consts.GRANT_TYPE_CLIENT_CREDENTIALS.equalsIgnoreCase(grantType.trim())) {
            writeError(ctx, Oauth2Consts.ERROR_UNSUPPORTED_GRANT_TYPE);
            return;
        }
        String clientId = firstNonBlank(ctx.param(Oauth2Consts.PARAM_CLIENT_ID),
                ctx.param(Oauth2Consts.PARAM_CLIENT_ID_CAMEL));
        String clientSecret = firstNonBlank(ctx.param(Oauth2Consts.PARAM_CLIENT_SECRET),
                ctx.param(Oauth2Consts.PARAM_CLIENT_SECRET_CAMEL));
        String authorization = ctx.header(Oauth2Consts.HEADER_AUTHORIZATION);
        if (needsBasicAuth(clientId, clientSecret, authorization)) {
            try {
                String decoded = new String(Base64.getDecoder()
                        .decode(authorization.substring(Oauth2Consts.BASIC_PREFIX.length()).trim()));
                int idx = decoded.indexOf(Oauth2Consts.COLON);
                if (idx > 0) {
                    clientId = firstNonBlank(clientId, decoded.substring(0, idx));
                    clientSecret = firstNonBlank(clientSecret, decoded.substring(idx + 1));
                }
            } catch (IllegalArgumentException ignored) {
                // 头不是合法的 Base64：按未携带客户端凭证处理，由服务层回 invalid_client
            }
        }
        String scope = firstNonBlank(ctx.param(Oauth2Consts.PARAM_SCOPE), null);
        try {
            Oauth2TokenVO token = tokenService.issueClientCredentials(clientId, clientSecret, scope);
            writeJson(ctx, Oauth2Consts.HTTP_OK, token);
        } catch (BizException e) {
            writeError(ctx, e.getMessage());
        }
    }

    /** 错误响应同样是 RFC 6749 定义的结构化对象，不走 Result 外壳 */
    private static void writeError(Context ctx, String message) {
        String description = message == null ? Oauth2Consts.ERROR_INVALID_REQUEST : message;
        String error = switch (description) {
            case Oauth2Consts.ERROR_UNSUPPORTED_GRANT_TYPE, Oauth2Consts.ERROR_INVALID_CLIENT,
                 Oauth2Consts.ERROR_INVALID_SCOPE -> description;
            default -> Oauth2Consts.ERROR_INVALID_REQUEST;
        };
        writeJson(ctx, Oauth2Consts.HTTP_BAD_REQUEST, new Oauth2ErrorVO(error, description));
    }

    private static void writeJson(Context ctx, int status, Object body) {
        ctx.status(status);
        ctx.contentType(MimeType.APPLICATION_JSON_VALUE);
        ctx.output(JsonUtil.toJson(body));
        ctx.setRendered(true);
    }

    /** 参数未给全客户端凭证，且请求头是 Basic 认证时才需要解析头部 */
    private static boolean needsBasicAuth(String clientId, String clientSecret, String authorization) {
        if (clientId != null && clientSecret != null) {
            return false;
        }
        return isBasicAuth(authorization);
    }

    private static boolean isBasicAuth(String authorization) {
        return authorization != null
                && authorization.regionMatches(true, 0, Oauth2Consts.BASIC_PREFIX, 0, Oauth2Consts.BASIC_PREFIX.length());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
