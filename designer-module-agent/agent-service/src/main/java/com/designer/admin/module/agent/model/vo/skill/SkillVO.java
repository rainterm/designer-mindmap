package com.designer.admin.module.agent.model.vo.skill;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 视图对象
 *
 * @author 雨夜终端
 */
@Data
public class SkillVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("编码")
    private String code;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("SKILL.md 内容")
    private String content;

    @ApiModelProperty("来源类型：manual / package")
    private String sourceType;

    @ApiModelProperty("包名")
    private String packageName;

    @ApiModelProperty("包大小（字节）")
    private Long packageSize;

    @ApiModelProperty("包内文件列表")
    private List<String> files;

    @ApiModelProperty("Skill 落盘路径")
    private String skillPath;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
