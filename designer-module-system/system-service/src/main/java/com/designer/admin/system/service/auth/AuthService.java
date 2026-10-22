package com.designer.admin.system.service.auth;

import com.designer.admin.system.model.dto.auth.LoginDTO;
import com.designer.admin.system.model.vo.auth.CaptchaVO;
import com.designer.admin.system.model.vo.auth.LoginVO;
import com.designer.admin.system.model.vo.auth.UserInfoVO;
import com.designer.admin.system.model.vo.rbac.RouterVO;
import org.noear.solon.core.handle.Context;

import java.util.List;

/**
 * 认证服务：登录 / 登出 / 当前用户信息 / 动态路由
 *
 * @author 雨夜终端
 */
public interface AuthService {

    /**
     * 账号密码登录：人机验证、失败锁定、BCrypt 校验、记录登录日志、写入 Sa-Token 会话。
     *
     * @param dto 登录表单：用户名、密码与可选的人机验证凭据
     * @param ctx 当前请求上下文，用于取 IP 与 User-Agent（可为 null）
     * @return 登录结果：token、有效期与用户信息
     */
    LoginVO login(LoginDTO dto, Context ctx);

    /**
     * 由已有 token 组装登录结果，供 SSO 换票等「会话已存在」的场景复用。
     *
     * @param token Sa-Token 的 token 值
     * @return 登录结果
     */
    LoginVO toLoginVo(String token);

    /**
     * 登录前询问该账号是否需要人机验证。
     *
     * <p>验证码本身由 AJ-Captcha 的 {@code /app-api/captcha/get} 下发，
     * 这里只回答「要不要」，前端据此决定是否展示滑块。</p>
     *
     * @param username 登录账号
     * @return 是否需要滑块验证
     */
    CaptchaVO getCaptcha(String username);

    /**
     * 退出登录，清理当前会话。
     */
    void logout();

    /**
     * 当前登录用户的信息（含角色与权限标识）。
     *
     * @return 用户信息
     */
    UserInfoVO getUserInfo();

    /**
     * 当前登录用户的动态菜单路由。
     *
     * @return 路由树
     */
    List<RouterVO> listRouters();
}
