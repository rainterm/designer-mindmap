package com.designer.admin.module.agent.model.dto.knowledge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 知识库分块更新参数
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeChunkUpdateDTO {

    @ApiModelProperty(value = "分片内容", required = true)
    @NotBlank(message = "分块内容不能为空")
    @Length(max = 20000, message = "分块内容长度不能超过 20000 个字符")
    private String content;
}
