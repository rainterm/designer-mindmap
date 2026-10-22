package com.designer.admin.system.model.entity.auth;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.util.List;

/**
 * OAuth2 客户端（Client Credentials）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oauth_client")
public class SysOauthClient extends BaseEntity {

    /** 客户端标识（唯一），对应 token 请求的 client_id */
    private String clientId;

    /** 客户端密钥（BCrypt），永不返回前端 */
    @ONodeAttr(ignore = true)
    private String clientSecret;

    /** 客户端名称，便于后台辨识 */
    private String clientName;

    /** access_token 有效期（秒） */
    private Integer accessTokenTtl;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 已绑定的 Scope ID 列表（非库字段） */
    @TableField(exist = false)
    private List<Long> scopeIds;

    /** 已绑定的 Scope 标识列表（非库字段） */
    @TableField(exist = false)
    private List<String> scopeCodes;
}
