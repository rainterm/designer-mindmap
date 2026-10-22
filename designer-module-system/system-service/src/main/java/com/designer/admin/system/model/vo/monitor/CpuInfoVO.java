package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * CPU 概况。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class CpuInfoVO {

    /** 操作系统架构名称 */
    private String name;

    /** CPU 架构 */
    private String arch;

    /** 可用处理器核数 */
    private int availableProcessors;

    /** 系统平均负载（操作系统不支持时为 -1） */
    private double systemLoadAverage;

    /** 整机 CPU 使用率（%），取不到时为 null */
    private Double systemUsage;

    /** 本进程 CPU 使用率（%），取不到时为 null */
    private Double processUsage;
}
