package com.designer.admin.module.agent.model.dto.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式对话结果
 *
 * @author 雨夜终端
 */
@Data
public class ChatStreamResult {

    @ApiModelProperty("助手回复正文")
    private String content = "";

    @ApiModelProperty("模型思考过程")
    private String thinking = "";

    @ApiModelProperty("工具调用过程列表")
    private List<ChatToolCallDTO> tools = new ArrayList<>();
}
