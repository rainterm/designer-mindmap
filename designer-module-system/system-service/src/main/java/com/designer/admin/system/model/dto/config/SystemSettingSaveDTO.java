package com.designer.admin.system.model.dto.config;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 系统设置保存
 *
 * @author 雨夜终端
 */
@Data
public class SystemSettingSaveDTO {

    @ApiModelProperty(value = "系统名称", required = true)
    @NotBlank(message = "系统名称不能为空")
    @Length(max = 50, message = "系统名称长度不能超过 50 个字符")
    private String systemName;

    @ApiModelProperty("登录页副标题，留空则不显示")
    @Length(max = 100, message = "登录页副标题长度不能超过 100 个字符")
    private String loginSubtitle;

    @ApiModelProperty("Logo 地址，留空则使用内置 Logo")
    @Length(max = 500, message = "Logo 地址长度不能超过 500 个字符")
    private String logoUrl;

    @ApiModelProperty("Logo 圆角像素，0 为直角")
    private Integer logoRadius;

    @ApiModelProperty("默认布局模式")
    private String defaultLayoutMode;

    @ApiModelProperty("默认主题色（十六进制，如 #16A34A）")
    private String defaultPrimaryColor;
}
