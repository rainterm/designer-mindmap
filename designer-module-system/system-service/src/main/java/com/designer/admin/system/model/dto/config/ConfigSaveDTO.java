package com.designer.admin.system.model.dto.config;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 参数配置保存
 *
 * @author 雨夜终端
 */
@Data
public class ConfigSaveDTO {

    @ApiModelProperty("参数 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "参数名称", required = true)
    @NotBlank(message = "参数名称不能为空")
    private String configName;

    @ApiModelProperty(value = "参数键名，全局唯一", required = true)
    @NotBlank(message = "参数键名不能为空")
    private String configKey;

    @ApiModelProperty(value = "参数键值", required = true)
    @NotBlank(message = "参数键值不能为空")
    private String configValue;

    @ApiModelProperty("参数类型：Y内置 N自定义，新增默认 N")
    private String configType;

    @ApiModelProperty("备注")
    private String remark;
}
