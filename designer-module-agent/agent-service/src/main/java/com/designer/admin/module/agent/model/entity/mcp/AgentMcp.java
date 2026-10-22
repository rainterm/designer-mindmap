package com.designer.admin.module.agent.model.entity.mcp;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_mcp")
public class AgentMcp extends BaseEntity {

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 传输方式：stdio / sse */
    private String transport;

    /** stdio 启动命令 */
    private String command;

    /** stdio 启动参数 */
    private String args;

    /** URL */
    private String url;

    /** 环境变量 JSON */
    private String envJson;

    /** 探测到的工具列表 JSON */
    private String toolsJson;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
