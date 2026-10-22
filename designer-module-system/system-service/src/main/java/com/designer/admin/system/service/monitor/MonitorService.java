package com.designer.admin.system.service.monitor;

import com.designer.admin.system.model.vo.monitor.ServerInfoVO;

/**
 * 服务监控：CPU / 内存 / JVM / 磁盘
 *
 * @author 雨夜终端
 */
public interface MonitorService {

    /**
     * 采集服务器运行状态：CPU、内存、JVM、系统信息与磁盘使用率。
     *
     * @return 监控总览（cpu / mem / jvm / sys / disks / time）
     */
    ServerInfoVO getServerInfo();
}
