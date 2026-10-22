package com.designer.admin.module.agent.model.dto.knowledge;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 知识库文档查询参数
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocQueryDTO extends PageQuery {

    @ApiModelProperty(value = "知识库 ID", required = true)
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeId;

    @ApiModelProperty("标题/文件名/URL 关键词")
    private String keyword;

    @ApiModelProperty("文档状态：ready/indexing/pending/failed")
    private String status;

    @ApiModelProperty("来源类型：upload/text/url")
    private String sourceType;
}
