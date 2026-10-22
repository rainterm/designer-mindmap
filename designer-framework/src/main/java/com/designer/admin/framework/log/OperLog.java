package com.designer.admin.framework.log;

import org.noear.solon.annotation.Around;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，由 {@link OperLogInterceptor} 环绕记录。
 *
 * @author 雨夜终端
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Around(OperLogInterceptor.class)
public @interface OperLog {

    /** 操作模块 */
    String title() default "";

    /** 业务类型，取值见 {@link BusinessType} */
    String businessType() default BusinessType.OTHER;

    /** 是否记录请求参数 */
    boolean saveParam() default true;

    /** 是否记录返回结果 */
    boolean saveResult() default false;
}
