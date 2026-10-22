package com.designer.admin.system.model.dto.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 岗位保存（新增/修改通用）
 *
 * @author 雨夜终端
 */
@Data
public class PostSaveDTO {

    @ApiModelProperty("岗位 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "岗位名称", required = true)
    @NotBlank(message = "岗位名称不能为空")
    private String postName;

    @ApiModelProperty(value = "岗位编码，全局唯一", required = true)
    @NotBlank(message = "岗位编码不能为空")
    private String postCode;

    @ApiModelProperty("岗位级别")
    private Integer postLevel;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
