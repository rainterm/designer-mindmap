package com.designer.admin.system.model.dto.auth;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * SSO 票据换 token
 *
 * @author 雨夜终端
 */
@Data
public class SsoExchangeDTO {

    @ApiModelProperty(value = "一次性票据", required = true)
    @NotBlank(message = "票据不能为空")
    private String ticket;
}
