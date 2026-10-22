package com.designer.admin.system.model.dto.auth;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SSO 客户端查询条件
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SsoClientQueryDTO extends PageQuery {

    @ApiModelProperty("客户端名称，模糊匹配")
    private String clientName;

    @ApiModelProperty("客户端标识，模糊匹配")
    private String clientKey;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
