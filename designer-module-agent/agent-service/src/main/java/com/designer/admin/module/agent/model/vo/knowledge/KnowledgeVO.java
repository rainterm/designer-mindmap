package com.designer.admin.module.agent.model.vo.knowledge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库视图对象
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("标签列表")
    private List<String> tags;

    @ApiModelProperty("RAG Embedding 供应商 ID")
    private Long embeddingProviderId;

    @ApiModelProperty("RAG Embedding 模型")
    private String embeddingModel;

    @ApiModelProperty("向量维度，首次索引后锁定")
    private Integer embeddingDim;

    @ApiModelProperty("分块策略：fixed / section_recursive")
    private String chunkStrategy;

    @ApiModelProperty("分块大小")
    private Integer chunkSize;

    @ApiModelProperty("分块重叠")
    private Integer chunkOverlap;

    @ApiModelProperty("检索 Top K")
    private Integer topK;

    @ApiModelProperty("相似度阈值")
    private BigDecimal scoreThreshold;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("文档数")
    private int docCount;

    @ApiModelProperty("分片数")
    private int chunkCount;

    @ApiModelProperty("字符数")
    private int charCount;

    @ApiModelProperty("已就绪文档数")
    private int readyDocCount;

    @ApiModelProperty("索引状态：empty/indexing/ready/failed")
    private String indexStatus;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
