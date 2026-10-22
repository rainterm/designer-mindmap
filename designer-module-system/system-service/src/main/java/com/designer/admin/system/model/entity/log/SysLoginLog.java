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
 * 登录日志（无逻辑删除，不继承 {@code BaseEntity}；按需异步写入）
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 客户端 IP */
    private String ip;

    /** 浏览器 User-Agent */
    private String browser;

    /** 操作系统 User-Agent */
    private String os;

    /** 登录结果：1成功 0失败 */
    private Integer status;

    /** 登录消息（失败原因或成功提示） */
    private String msg;

    /** 登录时间 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime loginTime;}