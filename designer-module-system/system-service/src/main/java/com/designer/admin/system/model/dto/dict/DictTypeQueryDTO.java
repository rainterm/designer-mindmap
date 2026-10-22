package com.designer.admin.system.model.dto.dict;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询条件
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQueryDTO extends PageQuery {

    @ApiModelProperty("字典名称，模糊匹配")
    private String dictName;

    @ApiModelProperty("字典类型标识，模糊匹配")
    private String dictType;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
