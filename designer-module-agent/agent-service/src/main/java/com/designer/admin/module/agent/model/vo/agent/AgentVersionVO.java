package com.designer.admin.module.agent.model.vo.agent;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 智能体版本视图对象
 *
 * @author 雨夜终端
 */
@Data
public class AgentVersionVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("智能体 ID")
    private Long agentId;

    @ApiModelProperty("历史版本号")
    private Integer version;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("发布人")
    private String publishedBy;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("发布时间")
    private LocalDateTime publishedAt;

    @ApiModelProperty("详情接口返回：完整配置（AgentSaveDTO 结构）")
    private Object config;
}
