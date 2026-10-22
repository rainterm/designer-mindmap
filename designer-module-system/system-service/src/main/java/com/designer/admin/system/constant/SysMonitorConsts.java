package com.designer.admin.system.constant;

/**
 * 监控（服务器信息 / 健康检查 / 在线用户）的常量。
 *
 * @author 雨夜终端
 */
public final class SysMonitorConsts {

    /** 1 KB = 1024 字节 */
    public static final long BYTES_PER_KB = 1024L;

    /** 健康检查中上报的应用标识 */
    public static final String HEALTH_APP_NAME = "designer-server";

    /** 健康检查状态：正常 */
    public static final String HEALTH_STATUS_UP = "UP";

    private SysMonitorConsts() {
    }
}
