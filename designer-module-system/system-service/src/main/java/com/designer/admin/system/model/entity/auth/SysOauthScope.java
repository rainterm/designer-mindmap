package com.designer.admin.system.model.entity.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OAuth2 开放接口 Scope
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oauth_scope")
public class SysOauthScope extends BaseEntity {

    /** Scope 标识（唯一），随 token 下发，如 {@code blog:read} */
    private String scopeCode;

    /** Scope 名称，后台展示用 */
    private String scopeName;

    /** 该 Scope 可访问的接口路径通配符，如 {@code /app-api/blog/**} */
    private String pathPattern;

    /** 显示顺序，越小越靠前 */
    private Integer sort;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
