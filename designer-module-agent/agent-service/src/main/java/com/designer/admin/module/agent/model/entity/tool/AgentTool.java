package com.designer.admin.module.agent.model.entity.tool;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_tool")
public class AgentTool extends BaseEntity {

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 类型：builtin */
    private String type;

    /** 描述 */
    private String description;

    /** 参数 schema JSON */
    private String schemaJson;

    /** 状态：1启用 0停用 */
    private Integer status;
}
