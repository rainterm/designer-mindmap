package com.designer.admin.module.agent.model.vo.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 会话聊天记录视图对象
 *
 * @author 雨夜终端
 */
@Data
public class ChatRecordVO {

    @ApiModelProperty("用户消息 id")
    private Long id;

    @ApiModelProperty("会话 ID")
    private Long sessionId;

    @ApiModelProperty("智能体 ID")
    private Long agentId;

    @ApiModelProperty("智能体名称")
    private String agentName;

    @ApiModelProperty("question")
    private String question;

    @ApiModelProperty("answer")
    private String answer;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
