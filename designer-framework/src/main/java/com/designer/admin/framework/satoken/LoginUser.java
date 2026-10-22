package com.designer.admin.framework.satoken;

import cn.dev33.satoken.json.SaJsonType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 会话中保存的登录用户快照（写入 Sa-Token Session）
 *
 * <p>必须实现 {@link SaJsonType}：Sa-Token 1.46 的 snack4 JSON 模板对 Redis 中的
 * 多态对象按类型白名单反序列化，仅放行实现了该标记接口的类。
 *
 * @author 雨夜终端
 */
@Data
public class LoginUser implements SaJsonType, Serializable {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private boolean superAdmin;
}
