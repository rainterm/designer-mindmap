package com.designer.admin.system.model.dto.auth;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OAuth2 客户端分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OauthClientQueryDTO extends PageQuery {

    @ApiModelProperty("client_id，模糊匹配")
    private String clientId;

    @ApiModelProperty("客户端名称，模糊匹配")
    private String clientName;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
