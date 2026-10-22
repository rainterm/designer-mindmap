package com.designer.admin.system.model.vo.auth;

import lombok.Data;

import java.util.List;

/**
 * 登录结果
 *
 * @author 雨夜终端
 */
@Data
public class LoginVO {

    /** token 请求头名称 */
    private String tokenName;

    /** token 值 */
    private String token;

    /** 有效期（秒） */
    private Long expiresIn;

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像地址，空表示用默认头像 */
    private String avatar;

    /** 角色标识列表 */
    private List<String> roles;
}
