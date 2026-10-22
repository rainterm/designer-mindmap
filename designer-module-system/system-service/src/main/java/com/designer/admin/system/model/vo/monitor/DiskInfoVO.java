package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * 单个磁盘分区的使用情况。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class DiskInfoVO {

    /** 挂载路径 */
    private String path;

    /** 总容量（字节） */
    private long total;

    /** 已用容量（字节） */
    private long used;

    /** 可用容量（字节） */
    private long free;

    /** 总容量（可读） */
    private String totalHuman;

    /** 已用容量（可读） */
    private String usedHuman;

    /** 可用容量（可读） */
    private String freeHuman;

    /** 使用率（%） */
    private double usage;
}
