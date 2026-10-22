package com.designer.admin.module.agent.model.entity.workflow;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_workflow")
public class AgentWorkflow extends BaseEntity {

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 描述 */
    private String description;

    /** 节点 JSON */
    private String nodesJson;

    /** 连线 JSON */
    private String edgesJson;

    /** 草稿节点图：保存即更新，不影响运行（运行读 nodesJson） */
    private String draftNodesJson;

    /** 草稿连线图 */
    private String draftEdgesJson;

    /** 已发布版本号：仅发布时递增，运行/试跑以此为准 */
    private Integer version;

    /** 草稿版本号：保存即递增，不影响运行 */
    private Integer draftVersion;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
