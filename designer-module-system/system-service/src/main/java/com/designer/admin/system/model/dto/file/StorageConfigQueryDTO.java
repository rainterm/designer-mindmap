package com.designer.admin.system.model.dto.file;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 存储配置分页查询
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StorageConfigQueryDTO extends PageQuery {

    @ApiModelProperty("配置名称，模糊匹配")
    private String configName;

    @ApiModelProperty("存储类型：local / s3")
    private String storageType;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;

    @ApiModelProperty("访问类型：1公开 0私有，为空表示不限")
    private Integer isPublic;
}
