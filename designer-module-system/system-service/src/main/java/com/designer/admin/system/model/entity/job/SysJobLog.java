package com.designer.admin.system.model.entity.job;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务调度日志
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_job_log")
public class SysJobLog implements Serializable {

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID（任务被删除后仍保留快照） */
    private Long jobId;

    /** 任务名称（执行时快照） */
    private String jobName;

    /** 任务分组（执行时快照） */
    private String jobGroup;

    /** 调用目标（执行时快照） */
    private String invokeTarget;

    /** 执行摘要信息 */
    private String jobMessage;

    /** 1成功 0失败 */
    private Integer status;

    /** 异常堆栈（失败时记录，便于排查） */
    private String exceptionInfo;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 执行时间 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime createTime;
}
