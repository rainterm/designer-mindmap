package com.designer.admin.module.agent.model.dto.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * 工作流保存参数
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowSaveDTO {

    @ApiModelProperty("主键 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "名称", required = true)
    @NotBlank(message = "工作流名称不能为空")
    @Length(max = 100, message = "工作流名称长度不能超过 100 个字符")
    private String name;

    @ApiModelProperty(value = "编码", required = true)
    @NotBlank(message = "工作流编码不能为空")
    @Length(max = 64, message = "工作流编码长度不能超过 64 个字符")
    private String code;

    @ApiModelProperty("描述")
    @Length(max = 500, message = "描述长度不能超过 500 个字符")
    private String description;

    @ApiModelProperty("节点列表")
    private List<Map<String, Object>> nodes;

    @ApiModelProperty("连线列表")
    private List<Map<String, Object>> edges;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    @Length(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
