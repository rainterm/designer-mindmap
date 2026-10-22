package com.designer.admin.system.model.vo.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * 健康检查响应。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class HealthVO {

    /** 应用标识 */
    private String app;

    /** Solon 版本 */
    private String solon;

    /** 健康状态：UP */
    private String status;
}
