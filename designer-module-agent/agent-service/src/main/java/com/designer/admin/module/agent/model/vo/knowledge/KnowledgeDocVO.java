package com.designer.admin.module.agent.model.vo.knowledge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 知识库文档视图对象
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeDocVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("知识库 ID")
    private Long knowledgeId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("来源类型：upload/text/url")
    private String sourceType;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件类型")
    private String fileType;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("正文内容")
    private String content;

    @ApiModelProperty("URL")
    private String url;

    @ApiModelProperty("文档状态：ready/indexing/pending/failed")
    private String status;

    @ApiModelProperty("分片数")
    private Integer chunkCount;

    @ApiModelProperty("索引进度 0-100")
    private Integer indexProgress;

    @ApiModelProperty("字符数")
    private Integer charCount;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
