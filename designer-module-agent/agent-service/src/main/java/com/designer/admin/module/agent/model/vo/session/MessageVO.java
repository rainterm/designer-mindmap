package com.designer.admin.module.agent.model.vo.session;

import io.swagger.annotations.ApiModelProperty;
import com.designer.admin.module.agent.model.dto.session.ChatAttachmentDTO;
import com.designer.admin.module.agent.model.dto.session.ChatToolCallDTO;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话消息视图对象
 *
 * @author 雨夜终端
 */
@Data
public class MessageVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("会话 ID")
    private Long sessionId;

    @ApiModelProperty("角色：user/assistant/system")
    private String role;

    @ApiModelProperty("消息正文")
    private String content;

    @ApiModelProperty("附件列表")
    private List<ChatAttachmentDTO> attachments;

    @ApiModelProperty("工具调用过程")
    private List<ChatToolCallDTO> tools;

    @ApiModelProperty("模型思考过程")
    private String thinking;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}
