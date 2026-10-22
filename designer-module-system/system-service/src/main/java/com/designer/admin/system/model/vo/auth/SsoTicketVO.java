package com.designer.admin.system.model.vo.auth;

import lombok.Data;

/**
 * 一次性 SSO 票据
 *
 * @author 雨夜终端
 */
@Data
public class SsoTicketVO {

    /** 一次性票据（换取登录态时消费） */
    private String ticket;

    /** 有效期（秒） */
    private Long expiresIn;
}
