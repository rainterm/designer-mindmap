package com.designer.admin.module.agent.model.vo.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流版本视图对象
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowVersionVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("workflowId")
    private Long workflowId;

    @ApiModelProperty("历史版本号")
    private Integer version;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("详情场景填充；列表场景为 null")
    private List<Map<String, Object>> nodes;

    @ApiModelProperty("详情场景填充；列表场景为 null")
    private List<Map<String, Object>> edges;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("发布人")
    private String publishedBy;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("发布时间")
    private LocalDateTime publishedAt;
}

