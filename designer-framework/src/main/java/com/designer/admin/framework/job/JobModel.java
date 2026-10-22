package com.designer.admin.framework.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务运行时模型（framework 侧，与 sys_job 调度字段对齐）。
 *
 * @author 雨夜终端
 */
@Data
public class JobModel implements Serializable {

    /**
     * 固定序列化版本号。
     *
     * <p><b>必须显式声明</b>：JobModel 会随 JobDataMap 以 Java 序列化写入
     * {@code QRTZ_JOB_DETAILS.JOB_DATA}。不写这个字段时，JVM 会按类的结构算出隐式版本号，
     * 于是以后<b>加一个字段</b>就会让库里已有的任务反序列化失败（InvalidClassException）——
     * 表现为升级后定时任务全部读不出来，只能手工清表。声明成常量后，
     * 新增字段是兼容变更，旧数据仍可读出（新字段为 null）。</p>
     */
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer misfirePolicy;
    private Integer concurrent;
    private Integer status;
}
