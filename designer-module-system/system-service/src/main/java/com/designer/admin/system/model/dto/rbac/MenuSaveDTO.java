package com.designer.admin.system.model.dto.rbac;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 菜单保存（新增/修改通用）
 *
 * @author 雨夜终端
 */
@Data
public class MenuSaveDTO {

    @ApiModelProperty("菜单 ID，新增时留空")
    private Long id;

    @ApiModelProperty("父菜单 ID，留空或 0 表示顶级菜单")
    private Long parentId;

    @ApiModelProperty(value = "菜单名称", required = true)
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @ApiModelProperty(value = "菜单类型：M目录 C菜单 F按钮", required = true)
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @ApiModelProperty("路由地址")
    private String path;

    @ApiModelProperty("前端组件路径")
    private String component;

    @ApiModelProperty("权限标识，如 system:user:list")
    private String perms;

    @ApiModelProperty("图标名")
    private String icon;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("是否显示：0隐藏 1显示")
    private Integer visible;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
