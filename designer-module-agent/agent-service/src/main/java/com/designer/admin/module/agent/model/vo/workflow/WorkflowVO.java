package com.designer.admin.module.agent.model.vo.workflow;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流视图对象
 *
 * @author 雨夜终端
 */
@Data
public class WorkflowVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("节点列表")
    private List<Map<String, Object>> nodes;

    @ApiModelProperty("连线列表")
    private List<Map<String, Object>> edges;

    @ApiModelProperty("开始节点入参定义（开放调用 params 的 key）")
    private List<WorkflowStartInputVO> startInputs;

    @ApiModelProperty("nodeCount")
    private int nodeCount;

    @ApiModelProperty("已发布版本号（运行/试跑以此为准）")
    private Integer version;

    @ApiModelProperty("草稿版本号（保存即递增）")
    private Integer draftVersion;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
