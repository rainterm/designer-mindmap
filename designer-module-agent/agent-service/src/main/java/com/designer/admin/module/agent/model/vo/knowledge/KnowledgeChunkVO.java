package com.designer.admin.module.agent.model.vo.knowledge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 知识库分块视图对象
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeChunkVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("文档 ID")
    private Long docId;

    @ApiModelProperty("分片序号")
    private Integer chunkIndex;

    @ApiModelProperty("分片正文")
    private String content;

    @ApiModelProperty("字符数")
    private Integer charCount;
}
