package com.designer.admin.module.agent.model.vo.knowledge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库检索测试结果
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeRetrieveTestVO {

    @ApiModelProperty("查询文本")
    private String query;

    @ApiModelProperty("最终 Top K")
    private int topK;

    @ApiModelProperty("实际向量召回候选数")
    private int recallTopK;

    @ApiModelProperty("相似度阈值（向量召回）")
    private double scoreThreshold;

    @ApiModelProperty("Rerank 供应商 ID")
    private Long rerankProviderId;

    @ApiModelProperty("Rerank 模型")
    private String rerankModel;

    @ApiModelProperty("是否启用了重排序")
    private boolean rerankEnabled;

    @ApiModelProperty("命中数量")
    private int hitCount;

    @ApiModelProperty("耗时（毫秒）")
    private long durationMs;

    @ApiModelProperty("检索命中列表")
    private List<Hit> hits = new ArrayList<>();

    /**
     * 检索命中
     *
     * @author 雨夜终端
     */
    @Data
    public static class Hit {

        @ApiModelProperty("知识库名称")
        private String kbName;

        @ApiModelProperty("知识库 ID")
        private Long knowledgeId;

        @ApiModelProperty("文档 ID")
        private Long docId;

        @ApiModelProperty("文档标题")
        private String docTitle;

        @ApiModelProperty("分片序号")
        private Integer chunkIndex;

        @ApiModelProperty("相似度分数")
        private Float score;

        @ApiModelProperty("内容")
        private String content;
    }
}
