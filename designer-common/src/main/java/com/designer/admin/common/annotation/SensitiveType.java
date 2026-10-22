package com.designer.admin.common.annotation;

/**
 * 敏感字段脱敏策略
 *
 * @author 雨夜终端
 */
public enum SensitiveType {

    /** 整段替换为占位符 */
    ALL,

    /** 手机号：保留前 3 后 4，如 138****5678 */
    PHONE,

    /** 邮箱：保留首字符与 @ 后域名，如 a***@example.com */
    EMAIL
}
