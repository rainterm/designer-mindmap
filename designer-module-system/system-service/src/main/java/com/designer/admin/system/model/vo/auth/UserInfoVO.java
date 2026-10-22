package com.designer.admin.system.model.vo.auth;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息（含角色与权限标识）
 *
 * @author 雨夜终端
 */
@Data
public class UserInfoVO {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像地址，空表示用默认头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色标识列表 */
    private List<String> roles;

    /** 权限标识列表；超级管理员为 ["*"] */
    private List<String> perms;

    /** 是否超级管理员 */
    private Boolean superAdmin;
}
