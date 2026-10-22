package com.designer.admin.module.agent.model.vo.agent;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体视图对象
 *
 * @author 雨夜终端
 */
@Data
public class AgentVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("头像地址")
    private String avatar;

    @ApiModelProperty("系统提示词")
    private String systemPrompt;

    @ApiModelProperty("模型供应商 ID")
    private Long providerId;

    @ApiModelProperty("模型供应商名称")
    private String providerName;

    @ApiModelProperty("模型编码")
    private String modelCode;

    @ApiModelProperty("温度")
    private BigDecimal temperature;

    @ApiModelProperty("Top P")
    private BigDecimal topP;

    @ApiModelProperty("最大输出 token；0=不限制")
    private Integer maxTokens;

    @ApiModelProperty("对话记忆轮数；0=不带历史")
    private Integer memoryRounds;

    @ApiModelProperty("绑定工具 ID 列表")
    private List<Long> toolIds;

    @ApiModelProperty("绑定 MCP ID 列表")
    private List<Long> mcpIds;

    @ApiModelProperty("绑定 Skill ID 列表")
    private List<Long> skillIds;

    @ApiModelProperty("绑定工作流 ID 列表")
    private List<Long> workflowIds;

    @ApiModelProperty("绑定知识库 ID 列表")
    private List<Long> knowledgeIds;

    @ApiModelProperty("RAG Embedding 供应商 ID")
    private Long embeddingProviderId;

    @ApiModelProperty("RAG Embedding 模型")
    private String embeddingModel;

    @ApiModelProperty("RAG 重排序供应商 ID")
    private Long rerankProviderId;

    @ApiModelProperty("RAG 重排序模型")
    private String rerankModel;

    @ApiModelProperty("RAG 最终 Top K（进入上下文条数）")
    private Integer ragTopK;

    @ApiModelProperty("向量召回候选数；0=自动")
    private Integer ragRecallTopK;

    @ApiModelProperty("RAG 相似度阈值（向量召回）")
    private BigDecimal ragScoreThreshold;

    @ApiModelProperty("是否支持聊天文件上传：1是 0否")
    private Integer fileEnabled;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("已发布版本号")
    private Integer version;

    @ApiModelProperty("草稿版本号")
    private Integer draftVersion;

    @ApiModelProperty("是否有未发布改动（draft_version > version）")
    private Boolean hasUnpublished;

    @ApiModelProperty("是否已开启公开访问链接：1是 0否")
    private Integer publicEnabled;

    @ApiModelProperty("公开访问 token（仅管理端返回；未开启为空）")
    private String publicToken;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
