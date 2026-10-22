package com.designer.admin.module.agent.model.dto.knowledge;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotNull;

/**
 * 知识库文档保存参数
 *
 * @author 雨夜终端
 */
@Data
public class KnowledgeDocSaveDTO {

    @ApiModelProperty("主键 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "知识库 ID", required = true)
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeId;

    @ApiModelProperty("标题")
    @Length(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @ApiModelProperty("来源类型：upload/text/url")
    @Length(max = 32, message = "来源类型长度不能超过 32 个字符")
    private String sourceType;

    @ApiModelProperty("文件名")
    @Length(max = 255, message = "文件名长度不能超过 255 个字符")
    private String fileName;

    @ApiModelProperty("文件类型")
    @Length(max = 32, message = "文件类型长度不能超过 32 个字符")
    private String fileType;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("正文内容")
    private String content;

    @ApiModelProperty("URL")
    @Length(max = 1000, message = "URL 长度不能超过 1000 个字符")
    private String url;

    @ApiModelProperty("文档状态：ready/indexing/pending/failed")
    @Length(max = 32, message = "文档状态长度不能超过 32 个字符")
    private String status;

    @ApiModelProperty("错误信息")
    @Length(max = 500, message = "错误信息长度不能超过 500 个字符")
    private String errorMsg;

    @ApiModelProperty("字符数")
    private Integer charCount;
}
