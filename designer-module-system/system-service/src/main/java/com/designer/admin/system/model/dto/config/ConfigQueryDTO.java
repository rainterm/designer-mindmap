package com.designer.admin.system.model.dto.config;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数配置查询
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQueryDTO extends PageQuery {

    @ApiModelProperty("参数名称，模糊匹配")
    private String configName;

    @ApiModelProperty("参数键名，模糊匹配")
    private String configKey;

    @ApiModelProperty("参数类型：Y内置 N自定义，为空表示不限")
    private String configType;
}
