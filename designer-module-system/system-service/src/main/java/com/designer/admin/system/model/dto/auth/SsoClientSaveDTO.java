package com.designer.admin.system.model.dto.auth;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * SSO 客户端保存（新增/修改通用）
 *
 * @author 雨夜终端
 */
@Data
public class SsoClientSaveDTO {

    @ApiModelProperty("客户端 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "客户端名称", required = true)
    @NotBlank(message = "客户端名称不能为空")
    private String clientName;

    @ApiModelProperty(value = "客户端标识，全局唯一", required = true)
    @NotBlank(message = "客户端标识不能为空")
    private String clientKey;

    @ApiModelProperty(value = "允许回跳的 Origin，用于换票时的来源校验", required = true)
    @NotBlank(message = "允许回跳 Origin 不能为空")
    private String origin;

    @ApiModelProperty(value = "打开地址", required = true)
    @NotBlank(message = "打开地址不能为空")
    private String openUrl;

    @ApiModelProperty("入口路径")
    private String entryPath;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
