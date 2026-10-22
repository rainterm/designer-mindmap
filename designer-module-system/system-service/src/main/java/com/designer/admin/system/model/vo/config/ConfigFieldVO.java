package com.designer.admin.system.model.vo.config;

import lombok.Builder;
import lombok.Data;

/**
 * 动态表单字段元数据（对外契约，不依赖 framework）。
 *
 * <p>存储配置与消息通道的配置项都由后端声明、前端据此渲染表单，
 * 因此这里只描述「字段长什么样」，不含任何取值。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class ConfigFieldVO {

    /** 字段名（保存时 configJson 中的 key） */
    private String name;

    /** 字段显示名称（表单 label） */
    private String label;

    /** 输入控件类型：text / password / number / switch / textarea */
    private String inputType;

    /** 是否必填 */
    private Boolean required;

    /** 输入框占位提示 */
    private String placeholder;

    /** 默认值，空表示无默认 */
    private String defaultValue;

    /** 字段说明（表单下方的帮助文案） */
    private String help;
}
