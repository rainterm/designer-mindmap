package com.designer.admin.system.model.entity.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统设置（单行，固定 id=1）：系统名称 / 登录页副标题 / Logo / 默认偏好设置
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_setting")
public class SysSetting extends BaseEntity {

    /** 系统名称（登录页 / 侧边栏 / 顶栏 / 浏览器标题） */
    private String systemName;

    /** 登录页副标题，留空则不显示 */
    private String loginSubtitle;

    /** 系统 Logo 地址，留空则使用内置 Logo */
    private String logoUrl;

    /** Logo 圆角（px，0 为直角） */
    private Integer logoRadius;

    /** 默认布局模式：vertical / dual-column / horizontal / side-nav / mixed-vertical / mixed-dual / content-full */
    private String defaultLayoutMode;

    /** 默认主题色（十六进制，如 #16A34A） */
    private String defaultPrimaryColor;
}
