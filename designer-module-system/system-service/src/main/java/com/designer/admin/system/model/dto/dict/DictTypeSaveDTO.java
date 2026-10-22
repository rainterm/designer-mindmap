package com.designer.admin.system.model.dto.dict;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 字典类型保存（新增/修改通用）
 *
 * @author 雨夜终端
 */
@Data
public class DictTypeSaveDTO {

    @ApiModelProperty("字典类型 ID，新增时留空")
    private Long id;

    @ApiModelProperty("字典名称")
    private String dictName;

    @ApiModelProperty("字典类型标识，全局唯一")
    private String dictType;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
