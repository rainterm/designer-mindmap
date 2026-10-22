package com.designer.admin.system.model.vo.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统设置（公开接口返回）：系统名称 / 登录页副标题 / Logo / 默认偏好设置
 *
 * @author 雨夜终端
 */
@Data
public class SystemSettingVO implements Serializable {

    /** 系统名称（登录页 / 侧边栏 / 顶栏 / 浏览器标题） */
    private String systemName;

    /** 登录页副标题，空表示不显示 */
    private String loginSubtitle;

    /** 系统 Logo 地址，空表示用内置 Logo */
    private String logoUrl;

    /** Logo 圆角（px） */
    private Integer logoRadius;

    /** 默认布局模式 */
    private String defaultLayoutMode;

    /** 默认主题色（十六进制） */
    private String defaultPrimaryColor;
}
