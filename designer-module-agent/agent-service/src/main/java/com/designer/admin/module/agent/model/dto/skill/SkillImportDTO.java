package com.designer.admin.module.agent.model.dto.skill;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

import java.util.List;

/**
 * Skill 导入参数
 *
 * @author 雨夜终端
 */
@Data
public class SkillImportDTO {

    @ApiModelProperty("名称")
    @Length(max = 100, message = "Skill 名称长度不能超过 100 个字符")
    private String name;

    @ApiModelProperty(value = "编码", required = true)
    @NotBlank(message = "Skill 编码不能为空")
    @Length(max = 64, message = "Skill 编码长度不能超过 64 个字符")
    private String code;

    @ApiModelProperty("描述")
    @Length(max = 500, message = "描述长度不能超过 500 个字符")
    private String description;

    @ApiModelProperty("SKILL.md 内容")
    private String content;

    @ApiModelProperty("包名")
    @Length(max = 200, message = "包名长度不能超过 200 个字符")
    private String packageName;

    @ApiModelProperty("包大小（字节）")
    private Long packageSize;

    @ApiModelProperty("包内文件列表")
    private List<String> files;

    @ApiModelProperty("Skill 路径")
    @Length(max = 500, message = "Skill 路径长度不能超过 500 个字符")
    private String skillPath;
}
