package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 服务器监控总览（首页仪表盘一次取全）。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class ServerInfoVO {

    /** CPU 概况 */
    private CpuInfoVO cpu;

    /** 物理内存 */
    private MemInfoVO mem;

    /** JVM 运行信息 */
    private JvmInfoVO jvm;

    /** 操作系统与运行环境 */
    private SysInfoVO sys;

    /** 磁盘分区使用情况 */
    private List<DiskInfoVO> disks;

    /** 采集时间（yyyy-MM-dd HH:mm:ss） */
    private String time;
}
