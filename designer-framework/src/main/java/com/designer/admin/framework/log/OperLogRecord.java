package com.designer.admin.framework.log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志写入模型（framework 侧，与 sys_oper_log 字段对齐，不依赖 system entity）。
 *
 * @author 雨夜终端
 */
@Data
public class OperLogRecord {

    private String title;
    private String businessType;
    private String method;
    private String requestMethod;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
