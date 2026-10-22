package com.designer.admin.system.model.dto.auth;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.common.annotation.SensitiveType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Email;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Pattern;

/**
 * 个人资料修改入参。
 *
 * <p>用户 ID 不放在 DTO 里：它来自登录态（{@code LoginHelper.getUserId()}），
 * 由服务端决定。若允许请求体传入，越权改他人资料的门就打开了。</p>
 *
 * @author 雨夜终端
 */
@Data
public class ProfileUpdateDTO {

    @ApiModelProperty(value = "昵称", required = true)
    @NotBlank(message = "昵称不能为空")
    @Length(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @ApiModelProperty("头像地址")
    private String avatar;

    @ApiModelProperty("邮箱")
    @Sensitive(type = SensitiveType.EMAIL)
    @Email(message = "邮箱格式不正确")
    @Length(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @ApiModelProperty("手机号")
    @Sensitive(type = SensitiveType.PHONE)
    @Pattern(value = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Length(max = 20, message = "手机号长度不能超过 20")
    private String phone;
}
