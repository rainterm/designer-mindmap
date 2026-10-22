package com.designer.admin.framework.spi;

import lombok.Builder;
import lombok.Data;

/**
 * 动态表单字段元数据：消息通道与文件存储的 Provider 共用。
 *
 * <p>两类 Provider 的配置模型完全相同 —— 「类型标识 + JSON 配置 + 前端按元数据渲染表单」，
 * 字段描述也一字不差（name / label / inputType / required / placeholder / defaultValue / help）。
 * 此前 message、storage 各有一份同构类，加字段时必须记得改两处；
 * 这里收敛成一个共享类型，两个功能包本身仍各自独立、互不依赖。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class ConfigFieldMeta {

    /** 字段名（写入 configJson 的 key） */
    private String name;

    /** 显示标签 */
    private String label;

    /** 控件类型：text / password / number / switch / textarea */
    private String inputType;

    /** 是否必填 */
    private boolean required;

    /** 占位提示 */
    private String placeholder;

    /** 默认值（字符串形式，前端自行转换） */
    private String defaultValue;

    /** 帮助说明 */
    private String help;
}
