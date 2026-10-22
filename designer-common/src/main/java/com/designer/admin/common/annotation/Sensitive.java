package com.designer.admin.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段：JSON 序列化时按 {@link #type()} 脱敏，反序列化不受影响。
 *
 * @author 雨夜终端
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Sensitive {

    /** 脱敏策略，默认整段替换 */
    SensitiveType type() default SensitiveType.ALL;

    /** {@link SensitiveType#ALL} 时使用的占位符 */
    String mask() default "******";
}
