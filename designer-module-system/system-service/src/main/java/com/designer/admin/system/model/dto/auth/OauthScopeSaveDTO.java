package com.designer.admin.system.model.dto.auth;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * OAuth2 授权范围新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class OauthScopeSaveDTO {

    @ApiModelProperty("范围 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "Scope 标识，全局唯一", required = true)
    @NotBlank(message = "Scope 标识不能为空")
    private String scopeCode;

    @ApiModelProperty(value = "Scope 名称", required = true)
    @NotBlank(message = "Scope 名称不能为空")
    private String scopeName;

    @ApiModelProperty(value = "放行的接口路径模式：* 单层、** 多层、{var} 路径变量", required = true)
    @NotBlank(message = "路径模式不能为空")
    private String pathPattern;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
