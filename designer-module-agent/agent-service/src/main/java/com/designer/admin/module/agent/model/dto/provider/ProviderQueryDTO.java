package com.designer.admin.module.agent.model.dto.provider;

import io.swagger.annotations.ApiModelProperty;
import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型供应商查询参数
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderQueryDTO extends PageQuery {

    @ApiModelProperty("名称（模糊）")
    private String name;

    @ApiModelProperty("Solon 方言：openai / ollama / anthropic 等")
    private String type;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
