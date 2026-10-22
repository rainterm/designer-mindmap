package com.designer.admin.system.model.dto.auth;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.system.util.PasswordValidatorUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Pattern;

/**
 * 个人改密入参。
 *
 * <p>用户 ID 不放在 DTO 里：它来自登录态（{@code LoginHelper.getUserId()}），
 * 由服务端决定。若允许请求体传入，越权改他人密码的门就打开了。</p>
 *
 * @author 雨夜终端
 */
@Data
public class PasswordUpdateDTO {

    @ApiModelProperty(value = "原密码", required = true)
    @Sensitive
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @ApiModelProperty(value = "新密码，需同时包含大小写字母、数字和特殊符号", required = true)
    @Sensitive
    @NotBlank(message = "新密码不能为空")
    @Length(min = 6, max = 50, message = "密码长度需在 6~50 个字符之间")
    @Pattern(value = PasswordValidatorUtil.STRENGTH_REGEXP, message = PasswordValidatorUtil.COMPLEXITY_MESSAGE)
    private String newPassword;
}
