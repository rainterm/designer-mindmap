package com.designer.admin.module.agent.model.entity.session;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 会话实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_session")
public class AgentSession extends BaseEntity {

    /** 智能体 ID */
    private Long agentId;

    /** 标题 */
    private String title;

    /** 消息条数 */
    private Integer messageCount;

    /** 最后消息时间 */
    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageAt;

    /** 状态：1启用 0停用 */
    private Integer status;
}
