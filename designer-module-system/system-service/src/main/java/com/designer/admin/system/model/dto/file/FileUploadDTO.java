package com.designer.admin.system.model.dto.file;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 文件上传的附加入参（文件本体由 {@code UploadedFile} 单独接收）。
 *
 * @author 雨夜终端
 */
@Data
public class FileUploadDTO {

    @ApiModelProperty("业务子目录，如 avatar；为空时落到 common")
    private String dir;

    @ApiModelProperty("指定存储配置 ID，为空表示默认配置")
    private Long configId;

    @ApiModelProperty("是否要求该配置为公开（登录页 Logo 等匿名可访问的资源）")
    private Boolean publicOnly;

    /**
     * 是否要求公开存储配置。
     *
     * @return {@code publicOnly} 为 {@code null} 时按 {@code false} 处理
     */
    public boolean requirePublic() {
        return Boolean.TRUE.equals(publicOnly);
    }
}
