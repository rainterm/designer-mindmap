package com.designer.admin.module.agent.model.dto.agent;

import io.swagger.annotations.ApiModelProperty;
import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体查询参数
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentQueryDTO extends PageQuery {

    @ApiModelProperty("名称/编码（模糊）")
    private String name;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
