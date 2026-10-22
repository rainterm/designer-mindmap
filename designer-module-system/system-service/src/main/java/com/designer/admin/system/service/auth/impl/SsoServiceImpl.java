package com.designer.admin.system.service.auth.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.constant.SsoConsts;
import com.designer.admin.system.constant.SystemRedisKeyConsts;
import com.designer.admin.system.model.vo.auth.LoginVO;
import com.designer.admin.system.model.vo.auth.SsoTicketVO;
import com.designer.admin.system.service.auth.AuthService;
import com.designer.admin.system.service.auth.SsoService;
import com.designer.admin.system.service.auth.SysSsoClientService;
import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.UUID;

/**
 * 跨应用单点登录：已登录端签发一次性票据，目标端换取同一 Sa-Token。
 *
 * @author 雨夜终端
 */
@Component
public class SsoServiceImpl implements SsoService {

    @Inject
    private RedisClient redisClient;
    @Inject
    private AuthService authService;
    @Inject
    private SysSsoClientService ssoClientService;

    @Override
    public SsoTicketVO issueTicket() {
        return issueTicket(null);
    }

    @Override
    public SsoTicketVO issueTicket(String redirect) {
        StpUtil.checkLogin();
        if (redirect != null && !redirect.isBlank()) {
            ssoClientService.assertAllowedReturnUrl(redirect);
        }
        String token = StpUtil.getTokenValue();
        if (token == null || token.isBlank()) {
            throw new BizException("未登录");
        }
        int ttl = Math.max(SsoConsts.MIN_TICKET_TTL_SECONDS,
                Solon.cfg().getInt(SsoConsts.KEY_TICKET_TTL, SsoConsts.DEFAULT_TICKET_TTL_SECONDS));
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisClient.open(s -> s.key(SystemRedisKeyConsts.SSO_TICKET_PREFIX + ticket).expire(ttl).set(token));
        SsoTicketVO vo = new SsoTicketVO();
        vo.setTicket(ticket);
        vo.setExpiresIn((long) ttl);
        return vo;
    }

    @Override
    public LoginVO exchange(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BizException("缺少登录票据");
        }
        String key = SystemRedisKeyConsts.SSO_TICKET_PREFIX + ticket.trim();
        String token = redisClient.openAndGet(s -> {
            String val = s.key(key).get();
            if (val != null && !val.isBlank()) {
                s.key(key).delete();
            }
            return val;
        });
        if (token == null || token.isBlank()) {
            throw new BizException("登录票据无效或已过期");
        }
        return authService.toLoginVo(token);
    }
}
