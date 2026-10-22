package com.designer.admin.module.agent.model.dto.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;

/**
 * 聊天附件
 *
 * @author 雨夜终端
 */
@Data
public class ChatAttachmentDTO {

    @ApiModelProperty("附件文件名")
    @Length(max = 255, message = "附件文件名长度不能超过 255 个字符")
    private String name;

    @ApiModelProperty("附件访问地址")
    @Length(max = 1000, message = "附件地址长度不能超过 1000 个字符")
    private String url;

    @ApiModelProperty("Content-Type")
    @Length(max = 128, message = "Content-Type 长度不能超过 128 个字符")
    private String contentType;

    @ApiModelProperty("大小（字节）")
    private Long size;

    @ApiModelProperty("文本类文件由前端读取后传入，供模型参考")
    private String textContent;

    @ApiModelProperty("小图 base64（不含 data: 前缀），优先于 url 传给多模态模型")
    private String base64;
}
