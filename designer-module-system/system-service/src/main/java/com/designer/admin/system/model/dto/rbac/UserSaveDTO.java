package com.designer.admin.system.model.dto.rbac;

import com.designer.admin.common.annotation.Sensitive;
import com.designer.admin.common.annotation.SensitiveType;
import com.designer.admin.system.util.PasswordValidatorUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;
import org.noear.solon.validation.annotation.Pattern;

import java.util.List;

/**
 * 用户保存（新增/修改通用）
 *
 * @author 雨夜终端
 */
@Data
public class UserSaveDTO {

    @ApiModelProperty("用户 ID，新增时留空")
    private Long id;

    @ApiModelProperty(value = "登录账号", required = true)
    @NotBlank(message = "账号不能为空")
    private String username;

    @ApiModelProperty("密码：新增必填，修改留空表示不改；需同时包含大小写字母、数字和特殊符号")
    @Sensitive
    @Length(min = 6, max = 50, message = "密码长度需在 6~50 个字符之间")
    @Pattern(value = PasswordValidatorUtil.STRENGTH_REGEXP, message = PasswordValidatorUtil.COMPLEXITY_MESSAGE)
    private String password;

    @ApiModelProperty(value = "昵称", required = true)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @ApiModelProperty("头像地址")
    private String avatar;

    @ApiModelProperty("邮箱")
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;

    @ApiModelProperty("手机号")
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("所属部门 ID")
    private Long deptId;

    @ApiModelProperty("关联的角色 ID 列表")
    private List<Long> roleIds;

    @ApiModelProperty("关联的岗位 ID 列表")
    private List<Long> postIds;
}
