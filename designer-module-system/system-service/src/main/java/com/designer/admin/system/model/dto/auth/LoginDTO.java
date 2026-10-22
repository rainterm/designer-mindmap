package com.designer.admin.system.model.dto.auth;

import com.designer.admin.common.annotation.Sensitive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 登录入参
 *
 * <p>账号长度上限 50 与 {@code sys_user.username VARCHAR(50)} 对齐；
 * 前端登录页的校验用的是同一个值（见 admin-web 的 login/index.vue），改动时两边要一起改。</p>
 *
 * @author 雨夜终端
 */
@Data
public class LoginDTO {

    @ApiModelProperty(value = "登录账号", required = true, example = "")
    @NotBlank(message = "账号不能为空")
    @Length(max = 50, message = "账号长度不能超过 50 个字符")
    private String username;

    @ApiModelProperty(value = "登录密码", required = true)
    @Sensitive
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 50, message = "密码长度需在 6~50 个字符之间")
    private String password;

    @ApiModelProperty("滑块验证码的二次校验凭据，由 POST /app-api/captcha/check 下发，一次性有效；仅在验证码开启时必填")
    private String captchaVerification;
}
