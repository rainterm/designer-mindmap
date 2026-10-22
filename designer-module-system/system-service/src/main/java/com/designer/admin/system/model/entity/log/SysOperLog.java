package com.designer.admin.system.model.entity.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（无逻辑删除，不继承 {@code BaseEntity}；由 {@code OperLogInterceptor} AOP 异步落库）
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作模块 */
    private String title;

    /** 业务类型，取值见 {@code BusinessType} 枚举 */
    private String businessType;

    /** 方法名称（Controller#method） */
    private String method;

    /** 请求方式：GET/POST/PUT/DELETE */
    private String requestMethod;

    /** 操作人账号 */
    private String operName;

    /** 请求 URL */
    private String operUrl;

    /** 操作 IP */
    private String operIp;

    /** 请求参数（JSON） */
    private String operParam;

    /** 返回结果（JSON） */
    private String jsonResult;

    /** 操作结果：1成功 0失败 */
    private Integer status;

    /** 错误信息（异常 message） */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 操作时间 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime operTime;
}