package com.designer.admin.system.model.vo.open;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 开放接口连通性测试结果。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class OpenApiPingVO {

    /** 是否连通 */
    private boolean ok;

    /** 当前令牌所属客户端 ID */
    private String clientId;

    /** 当前令牌授予的 scope 列表 */
    private List<String> scopes;

    /** 服务器时间（毫秒时间戳） */
    private long serverTime;
}
