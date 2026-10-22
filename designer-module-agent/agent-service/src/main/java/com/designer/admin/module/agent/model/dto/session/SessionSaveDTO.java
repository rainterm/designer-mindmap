package com.designer.admin.module.agent.model.dto.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 创建调试会话参数
 *
 * @author 雨夜终端
 */
@Data
public class SessionSaveDTO {

    @ApiModelProperty(value = "智能体 ID", required = true)
    @NotNull(message = "智能体不能为空")
    private Long agentId;

    @ApiModelProperty("会话标题")
    @Length(max = 200, message = "会话标题长度不能超过 200 个字符")
    private String title;
}
