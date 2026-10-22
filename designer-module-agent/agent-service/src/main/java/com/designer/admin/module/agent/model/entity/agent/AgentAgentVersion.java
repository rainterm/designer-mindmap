package com.designer.admin.module.agent.model.entity.agent;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 智能体版本实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_agent_version")
public class AgentAgentVersion extends BaseEntity {

    /** 智能体 ID */
    private Long agentId;

    /** 已发布版本号（与主表 version 对齐） */
    private Integer version;

    /** 发布时完整配置快照（AgentSaveDTO JSON） */
    private String configJson;

    /** 名称 */
    private String name;

    /** 编码 */
    private String code;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 发布人 */
    private String publishedBy;

    /** 发布时间 */
    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
}
