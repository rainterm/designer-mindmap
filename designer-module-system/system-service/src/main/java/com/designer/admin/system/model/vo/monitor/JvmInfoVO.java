package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * JVM 运行信息。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class JvmInfoVO {

    /** JVM 名称 */
    private String name;

    /** JVM 版本 */
    private String version;

    /** JVM 厂商 */
    private String vendor;

    /** Java 运行时版本 */
    private String javaVersion;

    /** 启动时间（yyyy-MM-dd HH:mm:ss） */
    private String startTime;

    /** 已运行时长 */
    private String runTime;

    /** 启动参数 */
    private String inputArgs;

    /** 堆已提交（字节） */
    private long heapTotal;

    /** 堆已用（字节） */
    private long heapUsed;

    /** 堆上限（字节），未设置时为 -1 */
    private long heapMax;

    /** 堆已提交（可读） */
    private String heapTotalHuman;

    /** 堆已用（可读） */
    private String heapUsedHuman;

    /** 堆上限（可读） */
    private String heapMaxHuman;

    /** 堆使用率（%） */
    private double heapUsage;

    /** 非堆已用（字节） */
    private long nonHeapUsed;

    /** 非堆已用（可读） */
    private String nonHeapUsedHuman;

    /** 当前线程数 */
    private int threadCount;

    /** 峰值线程数 */
    private int peakThreadCount;
}
