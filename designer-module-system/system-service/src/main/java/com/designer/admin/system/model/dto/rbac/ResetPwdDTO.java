package com.designer.admin.system.model.dto.rbac;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.system.util.PasswordValidatorUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Pattern;

/**
 * 重置密码
 *
 * @author 雨夜终端
 */
@Data
public class ResetPwdDTO {

    @ApiModelProperty(value = "目标用户 ID", required = true)
    private Long userId;

    @ApiModelProperty(value = "新密码，需同时包含大小写字母、数字和特殊符号", required = true)
    @Sensitive
    @NotBlank(message = "新密码不能为空")
    @Length(min = 6, max = 50, message = "密码长度需在 6~50 个字符之间")
    @Pattern(value = PasswordValidatorUtil.STRENGTH_REGEXP, message = PasswordValidatorUtil.COMPLEXITY_MESSAGE)
    private String password;
}
