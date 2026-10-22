package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * 物理内存概况。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class MemInfoVO {

    /** 总内存（字节） */
    private long total;

    /** 已用内存（字节） */
    private long used;

    /** 空闲内存（字节） */
    private long free;

    /** 总内存（可读） */
    private String totalHuman;

    /** 已用内存（可读） */
    private String usedHuman;

    /** 空闲内存（可读） */
    private String freeHuman;

    /** 使用率（%） */
    private double usage;
}
