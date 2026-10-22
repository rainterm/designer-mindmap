package com.designer.admin.system.service.auth;

import com.designer.admin.system.model.vo.auth.LoginVO;
import com.designer.admin.system.model.vo.auth.SsoTicketVO;

/**
 * 跨应用单点登录：已登录端签发一次性票据，目标端换取同一 Sa-Token。
 *
 * @author 雨夜终端
 */
public interface SsoService {

    /**
     * 为当前登录用户签发一次性 SSO 票据（不限制目标端）。
     *
     * @return 票据与有效期
     */
    SsoTicketVO issueTicket();

    /**
     * 为当前登录用户签发一次性 SSO 票据，并校验回跳地址是否可信。
     *
     * @param redirect 目标端回跳地址；传入时校验 Origin 是否在启用的 SSO 客户端白名单内，为空则不校验
     * @return 票据与有效期
     */
    SsoTicketVO issueTicket(String redirect);

    /**
     * 用一次性票据换取登录态，票据核销后立即失效。
     *
     * @param ticket 待核销的票据
     * @return 登录结果
     */
    LoginVO exchange(String ticket);
}
