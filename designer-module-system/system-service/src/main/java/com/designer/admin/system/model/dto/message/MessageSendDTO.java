package com.designer.admin.system.model.dto.message;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 测试 / 手动发送消息
 *
 * @author 雨夜终端
 */
@Data
public class MessageSendDTO {

    @ApiModelProperty("指定配置 ID，为空则按 channelType 取默认配置")
    private Long configId;

    @ApiModelProperty("通道类型，configId 为空时必填")
    private String channelType;

    @ApiModelProperty("收件人列表")
    private List<String> to;

    @ApiModelProperty("抄送人列表")
    private List<String> cc;

    @ApiModelProperty("消息标题")
    private String title;

    @ApiModelProperty("消息正文")
    private String content;

    @ApiModelProperty("正文是否为 HTML：true是 false否")
    private Boolean html;
}
