package com.designer.admin.system.service.auth.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.captcha.CaptchaVerifier;
import com.designer.admin.framework.client.ClientIpUtil;
import com.designer.admin.framework.crypto.PasswordUtil;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.LoginUser;
import com.designer.admin.framework.satoken.PermissionLoader;
import com.designer.admin.framework.satoken.PermissionSnapshot;
import com.designer.admin.system.constant.OnlineUserConsts;
import com.designer.admin.system.constant.UserAgentConsts;
import com.designer.admin.system.mapper.rbac.SysUserMapper;
import com.designer.admin.system.model.dto.auth.LoginDTO;
import com.designer.admin.system.model.entity.log.SysLoginLog;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.vo.auth.CaptchaVO;
import com.designer.admin.system.model.vo.auth.LoginVO;
import com.designer.admin.system.model.vo.auth.UserInfoVO;
import com.designer.admin.system.model.vo.rbac.RouterVO;
import com.designer.admin.system.service.auth.AuthService;
import com.designer.admin.system.service.auth.LoginAttemptService;
import com.designer.admin.system.service.config.SysConfigService;
import com.designer.admin.system.service.log.SysLoginLogService;
import com.designer.admin.system.service.rbac.SysMenuService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 认证服务：登录 / 登出 / 当前用户信息 / 动态路由
 *
 * @author 雨夜终端
 */
@Component
public class AuthServiceImpl implements AuthService {

    @Db
    private SysUserMapper userMapper;
    @Inject
    private SysMenuService menuService;
    @Inject
    private SysLoginLogService loginLogService;
    @Inject
    private CaptchaVerifier captchaVerifier;
    @Inject
    private LoginAttemptService loginAttemptService;
    @Inject
    private SysConfigService configService;
    @Inject
    private PermissionLoader permissionLoader;

    @Override
    public LoginVO login(LoginDTO dto, Context ctx) {
        String ip = ClientIpUtil.normalize(ctx);
        String userAgent = ctx == null ? null : ctx.userAgent();
        // 账号大小写不敏感，与唯一性校验（库表 ci 排序规则）保持同一口径：
        // 入口统一归一为「去空白 + 转小写」，登录查询、失败计数、锁定键共用这个规范形式。
        // 于是 admin / Admin / ADMIN 必然落到同一个 Redis 键上，不存在变体绕过计数的问题。
        // 用 Locale.ROOT：toLowerCase() 不带参数会跟随默认区域，土耳其语环境下 I 会折成 ı。
        String username = dto.getUsername() == null
                ? "" : dto.getUsername().trim().toLowerCase(Locale.ROOT);

        // 先判锁定：锁定与验证码都只看「要不要继续」，lock 检查不消费任何凭据。
        // 放在验证码之前，避免锁定中的账号还要先解一次滑块才知道自己被锁了。
        loginAttemptService.assertNotLocked(username, ip);

        // 再问「要不要验」，最后才决定是否消费凭据：凭据一次性，
        // 对不需要验证的账号调校验会把它白白用掉（见 LoginAttemptService 的类注释）
        if (loginAttemptService.isCaptchaRequired(username)) {
            captchaVerifier.assertVerified(dto.getCaptchaVerification());
        }

        // 库表排序规则是 utf8mb4_general_ci，大小写变体同样会命中，无需再比原文
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            recordLoginLog(username, ip, userAgent, 0, "账号或密码错误");
            try {
                loginAttemptService.saveLoginFail(username, ip);
            } catch (BizException lockEx) {
                recordLoginLog(username, ip, userAgent, 0, lockEx.getMessage());
                throw lockEx;
            }
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != SystemConsts.STATUS_ENABLE) {
            recordLoginLog(username, ip, userAgent, 0, "账号已停用");
            throw new BizException("账号已停用，请联系管理员");
        }

        loginAttemptService.removeLoginFail(username, ip);
        long tokenTimeout = configService.getTokenTimeoutSeconds();
        long activeTimeout = configService.getTokenActiveTimeoutSeconds();
        SaLoginParameter loginParam = new SaLoginParameter().setTimeout(tokenTimeout);
        if (activeTimeout > 0) {
            loginParam.setActiveTimeout(activeTimeout);
        }
        StpUtil.login(user.getId(), loginParam);

        PermissionSnapshot snapshot = permissionLoader.load(user.getId());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRoles(snapshot.getRoles());
        loginUser.setSuperAdmin(Boolean.TRUE.equals(snapshot.getSuperAdmin()));
        StpUtil.getSession().set(LoginHelper.LOGIN_USER_KEY, loginUser);

        // 写入 token 会话，供在线用户列表展示
        StpUtil.getTokenSession().set(OnlineUserConsts.TOKEN_LOGIN_IP, ip);
        StpUtil.getTokenSession().set(OnlineUserConsts.TOKEN_BROWSER, parseBrowser(userAgent));
        StpUtil.getTokenSession().set(OnlineUserConsts.TOKEN_OS, parseOs(userAgent));
        StpUtil.getTokenSession().set(OnlineUserConsts.TOKEN_LOGIN_TIME, System.currentTimeMillis());

        recordLoginLog(user.getUsername(), ip, userAgent, 1, "登录成功");
        return toLoginVo(StpUtil.getTokenValue());
    }

    @Override
    public LoginVO toLoginVo(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException("登录状态已失效，请重新登录");
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            throw new BizException("登录状态已失效，请重新登录");
        }
        Long userId = Long.parseLong(String.valueOf(loginId));
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在或已被删除");
        }
        List<String> roles = permissionLoader.load(user.getId()).getRoles();
        LoginVO vo = new LoginVO();
        vo.setTokenName(StpUtil.getTokenName());
        vo.setToken(token);
        vo.setExpiresIn(StpUtil.getTokenTimeout(token));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roles);
        return vo;
    }

    @Override
    public CaptchaVO getCaptcha(String username) {
        CaptchaVO vo = new CaptchaVO();
        // 计数键的归一化在 LoginAttemptService 内部完成，这里直接传原始输入
        vo.setEnabled(loginAttemptService.isCaptchaRequired(username));
        return vo;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserInfoVO getUserInfo() {
        Long userId = LoginHelper.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在或已被删除");
        }
        PermissionSnapshot snapshot = permissionLoader.load(userId);
        List<String> roles = snapshot.getRoles();
        boolean superAdmin = Boolean.TRUE.equals(snapshot.getSuperAdmin());

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRoles(roles);
        vo.setSuperAdmin(superAdmin);
        vo.setPerms(snapshot.getPermissions());
        return vo;
    }

    @Override
    public List<RouterVO> listRouters() {
        Long userId = LoginHelper.getUserId();
        boolean superAdmin = Boolean.TRUE.equals(permissionLoader.load(userId).getSuperAdmin());
        return menuService.listRouters(userId, superAdmin);
    }

    private void recordLoginLog(String username, String ip, String userAgent, int status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(ip);
        log.setBrowser(parseBrowser(userAgent));
        log.setOs(parseOs(userAgent));
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogService.addLoginLogAsync(log);
    }

    /**
     * 从 UA 解析浏览器标识，取值见 {@link UserAgentConsts}。
     *
     * <p>判断顺序不可调换：Edge 的 UA 同时含 {@code Chrome/}，Safari 的匹配串也出现在
     * Chrome / Edge 的 UA 中，故 Edge 先于 Chrome、Safari 放最后。</p>
     */
    private String parseBrowser(String ua) {
        if (ua == null) {
            return UserAgentConsts.UNKNOWN;
        }
        if (ua.contains(UserAgentConsts.TOKEN_EDGE)) {
            return UserAgentConsts.BROWSER_EDGE;
        }
        if (ua.contains(UserAgentConsts.TOKEN_CHROME)) {
            return UserAgentConsts.BROWSER_CHROME;
        }
        if (ua.contains(UserAgentConsts.TOKEN_FIREFOX)) {
            return UserAgentConsts.BROWSER_FIREFOX;
        }
        if (ua.contains(UserAgentConsts.TOKEN_SAFARI)) {
            return UserAgentConsts.BROWSER_SAFARI;
        }
        return UserAgentConsts.OTHER;
    }

    /**
     * 从 UA 解析操作系统标识，取值见 {@link UserAgentConsts}。
     *
     * <p>Android 的 UA 里带 {@code Linux}，因此 Android 必须先判断。</p>
     */
    private String parseOs(String ua) {
        if (ua == null) {
            return UserAgentConsts.UNKNOWN;
        }
        if (ua.contains(UserAgentConsts.TOKEN_WINDOWS)) {
            return UserAgentConsts.OS_WINDOWS;
        }
        if (ua.contains(UserAgentConsts.TOKEN_MAC_OS_X) || ua.contains(UserAgentConsts.TOKEN_MACINTOSH)) {
            return UserAgentConsts.OS_MACOS;
        }
        if (ua.contains(UserAgentConsts.TOKEN_ANDROID)) {
            return UserAgentConsts.OS_ANDROID;
        }
        if (ua.contains(UserAgentConsts.TOKEN_IPHONE) || ua.contains(UserAgentConsts.TOKEN_IPAD)) {
            return UserAgentConsts.OS_IOS;
        }
        if (ua.contains(UserAgentConsts.TOKEN_LINUX)) {
            return UserAgentConsts.OS_LINUX;
        }
        return UserAgentConsts.OTHER;
    }
}
