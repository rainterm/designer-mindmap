package com.designer.admin.module.agent.model.entity.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话消息实体
 *
 * @author 雨夜终端
 */
@Data
@TableName("agent_message")
public class AgentMessage implements Serializable {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID */
    private Long sessionId;

    /** 角色：user/assistant/system */
    private String role;

    /** 内容 */
    private String content;

    /** 附件列表 JSON */
    private String attachmentsJson;

    /** 工具调用过程 JSON */
    private String toolsJson;

    /** 模型思考过程 */
    private String thinking;

    /** 创建时间 */
    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
