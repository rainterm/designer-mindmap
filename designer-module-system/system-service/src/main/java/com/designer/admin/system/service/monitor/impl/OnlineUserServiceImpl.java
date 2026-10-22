package com.designer.admin.system.service.monitor.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.redis.RedisKeyConsts;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.framework.satoken.LoginUser;
import com.designer.admin.system.constant.OnlineUserConsts;
import com.designer.admin.system.constant.SystemRedisKeyConsts;
import com.designer.admin.system.model.dto.monitor.OnlineUserQueryDTO;
import com.designer.admin.system.model.vo.monitor.OnlineUserVO;
import com.designer.admin.system.service.monitor.OnlineUserService;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在线用户：遍历 Account-Session 终端列表 + 强退
 *
 * @author 雨夜终端
 */
@Component
public class OnlineUserServiceImpl implements OnlineUserService {

    @Inject
    private RedisClient redisClient;

    @Override
    public IPage<OnlineUserVO> listPage(OnlineUserQueryDTO query) {
        List<OnlineUserVO> all = listOnlineUsers(query == null ? null : query.getUsername(),
                query == null ? null : query.getIp());
        long total = all.size();
        long pageNum = query == null ? 1 : query.getPageNum();
        long pageSize = query == null ? 10 : query.getPageSize();
        long from = Math.max(0, (pageNum - 1) * pageSize);
        long to = Math.min(total, from + pageSize);
        List<OnlineUserVO> slice = from >= total ? List.of() : all.subList((int) from, (int) to);
        Page<OnlineUserVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(slice);
        return page;
    }

    @Override
    public List<OnlineUserVO> listOnlineUsers(String username, String ip) {
        String sessionPrefix = StpUtil.getTokenName() + RedisKeyConsts.SEPARATOR
                + StpUtil.getLoginType() + SystemRedisKeyConsts.SA_TOKEN_SESSION_SUFFIX;
        Set<String> sessionKeys;
        try {
            sessionKeys = redisClient.openAndGet(s -> s.keys(sessionPrefix + "*"));
        } catch (Exception e) {
            throw new BizException("读取在线会话失败: " + e.getMessage());
        }
        if (sessionKeys == null || sessionKeys.isEmpty()) {
            return List.of();
        }

        List<OnlineUserVO> result = new ArrayList<>(sessionKeys.size());
        Map<String, LoginUser> userCache = HashMap.newHashMap(sessionKeys.size());

        for (String sessionKey : sessionKeys) {
            String loginId = sessionKey.substring(sessionPrefix.length());
            if (loginId.isBlank()) {
                continue;
            }
            SaSession session;
            try {
                session = StpUtil.getSessionByLoginId(loginId, false);
            } catch (Exception e) {
                continue;
            }
            if (session == null) {
                continue;
            }

            LoginUser loginUser = userCache.computeIfAbsent(loginId, id -> {
                Object lu = session.get(LoginHelper.LOGIN_USER_KEY);
                return lu instanceof LoginUser u ? u : null;
            });

            String uname = loginUser != null ? loginUser.getUsername() : loginId;
            String nickname = loginUser != null ? loginUser.getNickname() : "";
            if (notBlank(username) && !contains(uname, username)) {
                continue;
            }

            List<SaTerminalInfo> terminals = session.getTerminalList();
            if (terminals == null || terminals.isEmpty()) {
                continue;
            }

            for (SaTerminalInfo terminal : terminals) {
                OnlineUserVO vo = buildOnlineUser(loginId, uname, nickname, terminal, ip);
                if (vo != null) {
                    result.add(vo);
                }
            }
        }

        result.sort(Comparator.comparing(OnlineUserVO::getLoginTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /**
     * 把一个终端信息转换成在线用户视图，无效终端返回 {@code null}。
     *
     * @param loginId    登录主体 id
     * @param username   账号
     * @param nickname   昵称
     * @param terminal   终端信息
     * @param ipFilter   IP 过滤条件，为空表示不过滤
     * @return 在线用户视图；终端已失效或不符合过滤条件时返回 {@code null}
     */
    private OnlineUserVO buildOnlineUser(String loginId, String username, String nickname,
                                         SaTerminalInfo terminal, String ipFilter) {
        if (terminal == null || !notBlank(terminal.getTokenValue())) {
            return null;
        }
        String token = terminal.getTokenValue();
        // 确认 token 仍有效（已被强退/过期的终端跳过）
        Object tokenLoginId;
        try {
            tokenLoginId = StpUtil.getLoginIdByToken(token);
        } catch (Exception e) {
            return null;
        }
        if (tokenLoginId == null || !String.valueOf(tokenLoginId).equals(loginId)) {
            return null;
        }

        OnlineUserVO vo = new OnlineUserVO();
        vo.setTokenId(token);
        try {
            vo.setUserId(Long.parseLong(loginId));
        } catch (NumberFormatException e) {
            return null;
        }
        vo.setUsername(username);
        vo.setNickname(nickname);
        fillTokenMeta(vo, token, terminal);

        if (notBlank(ipFilter) && !contains(vo.getIp(), ipFilter)) {
            return null;
        }
        try {
            vo.setTimeout(StpUtil.getTokenTimeout(token));
        } catch (Exception e) {
            vo.setTimeout(-1L);
        }
        return vo;
    }

    private void fillTokenMeta(OnlineUserVO vo, String token, SaTerminalInfo terminal) {
        // 不自动创建 Token-Session，避免无效 token 抛异常拖垮整次查询
        SaSession tokenSession = null;
        try {
            tokenSession = StpUtil.getStpLogic().getTokenSessionByToken(token, false);
        } catch (Exception ignored) {
            // ignore
        }
        if (tokenSession != null) {
            Object tip = tokenSession.get(OnlineUserConsts.TOKEN_LOGIN_IP);
            Object tb = tokenSession.get(OnlineUserConsts.TOKEN_BROWSER);
            Object tos = tokenSession.get(OnlineUserConsts.TOKEN_OS);
            Object tt = tokenSession.get(OnlineUserConsts.TOKEN_LOGIN_TIME);
            vo.setIp(tip == null ? "" : String.valueOf(tip));
            vo.setBrowser(tb == null ? "" : String.valueOf(tb));
            vo.setOs(tos == null ? "" : String.valueOf(tos));
            if (tt instanceof Number n) {
                vo.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(n.longValue()), ZoneId.systemDefault()));
                return;
            }
        }
        vo.setIp("");
        vo.setBrowser("");
        vo.setOs(terminal.getDeviceType() == null ? "" : terminal.getDeviceType());
        if (terminal.getCreateTime() > 0) {
            vo.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(terminal.getCreateTime()), ZoneId.systemDefault()));
        }
    }

    @Override
    public void forceLogout(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new BizException("token 不能为空");
        }
        String current = StpUtil.getTokenValue();
        if (tokenId.equals(current)) {
            throw new BizException("不能强退当前登录会话");
        }
        StpUtil.kickoutByTokenValue(tokenId);
    }

    @Override
    public void forceLogoutBatch(List<String> tokenIds) {
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        String current = StpUtil.getTokenValue();
        for (String tokenId : tokenIds) {
            if (tokenId == null || tokenId.isBlank() || tokenId.equals(current)) {
                continue;
            }
            StpUtil.kickoutByTokenValue(tokenId);
        }
    }

    /**
     * 判断字符串是否包含关键字（空值安全）。
     *
     * @param value   待匹配字符串
     * @param keyword 关键字
     * @return 包含关键字时返回 {@code true}
     */
    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
