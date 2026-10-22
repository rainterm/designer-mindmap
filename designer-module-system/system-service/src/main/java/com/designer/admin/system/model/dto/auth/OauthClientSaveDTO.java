package com.designer.admin.system.model.dto.auth;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.List;

/**
 * OAuth2 客户端新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class OauthClientSaveDTO {

    @ApiModelProperty("客户端 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "client_id，全局唯一", required = true)
    @NotBlank(message = "client_id 不能为空")
    private String clientId;

    @ApiModelProperty(value = "客户端名称", required = true)
    @NotBlank(message = "客户端名称不能为空")
    private String clientName;

    @ApiModelProperty("访问令牌有效期（秒）")
    private Integer accessTokenTtl;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("关联的授权范围 ID 列表")
    private List<Long> scopeIds;
}
