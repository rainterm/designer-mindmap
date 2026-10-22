package com.designer.admin.system.model.dto.file;

import com.designer.admin.common.annotation.Sensitive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.Map;

/**
 * 存储配置保存
 *
 * @author 雨夜终端
 */
@Data
public class StorageConfigSaveDTO {

    @ApiModelProperty("配置 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "配置名称", required = true)
    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @ApiModelProperty(value = "存储类型：local / s3", required = true)
    @NotBlank(message = "存储类型不能为空")
    private String storageType;

    @ApiModelProperty("是否设为默认：0否 1是")
    private Integer isDefault;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("访问类型：1公开 0私有；公开配置的下载 URL 走 /app-api，私有配置走 /admin-api")
    private Integer isPublic;

    @ApiModelProperty("允许上传的扩展名，逗号分隔；为空表示不限")
    private String allowedExtensions;

    @ApiModelProperty("单文件大小上限（字节）")
    private Long maxSize;

    @ApiModelProperty("上传图片是否压缩：1是 0否；留空时编辑保留原值")
    private Integer compressEnabled;

    @ApiModelProperty("JPEG 压缩质量 10-100，越小体积越小；PNG 为无损压缩、忽略该值")
    private Integer compressQuality;

    @ApiModelProperty("类型专属配置（含密钥，返回时整段脱敏）")
    @Sensitive
    private Map<String, Object> config;

    @ApiModelProperty("备注")
    private String remark;
}
