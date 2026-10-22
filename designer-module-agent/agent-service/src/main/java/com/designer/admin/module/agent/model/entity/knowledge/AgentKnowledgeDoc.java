package com.designer.admin.module.agent.model.entity.knowledge;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_knowledge_doc")
public class AgentKnowledgeDoc extends BaseEntity {

    /** 知识库 ID */
    private Long knowledgeId;

    /** 标题 */
    private String title;

    /** 来源类型：upload/text/url */
    private String sourceType;

    /** 文件名 */
    private String fileName;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 正文内容 */
    private String content;

    /** URL */
    private String url;

    /** ready / indexing / pending / failed */
    private String status;

    /** 分片数 */
    private Integer chunkCount;

    /** 索引进度 0-100 */
    private Integer indexProgress;

    /** 字符数 */
    private Integer charCount;

    /** 错误信息 */
    private String errorMsg;
}
