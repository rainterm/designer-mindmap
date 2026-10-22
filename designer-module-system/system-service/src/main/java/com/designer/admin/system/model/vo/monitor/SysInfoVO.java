package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * 操作系统与运行环境。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class SysInfoVO {

    /** 操作系统名称 */
    private String osName;

    /** 操作系统架构 */
    private String osArch;

    /** 操作系统版本 */
    private String osVersion;

    /** 工作目录 */
    private String userDir;

    /** 运行账户 */
    private String userName;

    /** 主机名，取不到时为 "-" */
    private String hostName;

    /** 主机 IP，取不到时为 "-" */
    private String hostIp;

    /** 应用名 */
    private String appName;

    /** Solon 版本 */
    private String solonVersion;
}
