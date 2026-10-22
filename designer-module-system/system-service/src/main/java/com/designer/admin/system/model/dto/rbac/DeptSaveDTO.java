package com.designer.admin.system.model.dto.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 部门保存
 *
 * @author 雨夜终端
 */
@Data
public class DeptSaveDTO {

    @ApiModelProperty("部门 ID，新增时留空")
    private Long id;

    @ApiModelProperty("父部门 ID，留空或 0 表示根部门")
    private Long parentId;

    @ApiModelProperty(value = "部门名称", required = true)
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
