package com.designer.admin.system.model.entity.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SSO 客户端：跨应用单点登录白名单与打开地址
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sso_client")
public class SysSsoClient extends BaseEntity {

    /** 客户端名称 */
    private String clientName;

    /** 客户端标识（全局唯一，如 app1） */
    private String clientKey;

    /** 允许回跳的 Origin */
    private String origin;

    /** 打开地址（一般与 Origin 同主机） */
    private String openUrl;

    /** SSO 接入路径，默认 /sso */
    private String entryPath;

    /** 显示顺序 */
    private Integer sort;

    /** 1正常 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
