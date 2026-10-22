package com.designer.admin.framework.job;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务调度日志模型（framework 侧）。
 *
 * @author 雨夜终端
 */
@Data
public class JobLogRecord {

    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String jobMessage;
    private Integer status;
    private String exceptionInfo;
    private Long costTime;
    private LocalDateTime createTime;
}
