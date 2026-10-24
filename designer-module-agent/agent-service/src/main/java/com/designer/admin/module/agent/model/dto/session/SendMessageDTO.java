package com.designer.admin.module.agent.model.dto.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;

import java.util.List;

/**
 * 发送对话消息参数
 *
 * @author 雨夜终端
 */
@Data
public class SendMessageDTO {

    @ApiModelProperty("用户输入内容（可与附件二选一）")
    @Length(max = 50000, message = "消息内容长度不能超过 50000 个字符")
    private String content;

    @ApiModelProperty("附件列表")
    private List<ChatAttachmentDTO> attachments;

    @ApiModelProperty("调试模式：true 时本次对话使用草稿配置，不影响已发布配置")
    private Boolean useDraft;

    @ApiModelProperty("导图编辑侧边对话：绑定的导图项目 ID（写入系统上下文，不进入用户消息）")
    private Long mmProjectId;
}
