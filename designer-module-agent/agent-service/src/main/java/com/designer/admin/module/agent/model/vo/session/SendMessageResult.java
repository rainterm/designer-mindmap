package com.designer.admin.module.agent.model.vo.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 发送消息结果
 *
 * @author 雨夜终端
 */
@Data
public class SendMessageResult {

    @ApiModelProperty("user")
    private MessageVO user;

    @ApiModelProperty("assistant")
    private MessageVO assistant;
}
