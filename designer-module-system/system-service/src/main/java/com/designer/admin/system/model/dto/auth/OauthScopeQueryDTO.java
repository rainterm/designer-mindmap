package com.designer.admin.system.model.dto.auth;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OAuth2 授权范围分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OauthScopeQueryDTO extends PageQuery {

    @ApiModelProperty("Scope 标识，模糊匹配")
    private String scopeCode;

    @ApiModelProperty("Scope 名称，模糊匹配")
    private String scopeName;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
