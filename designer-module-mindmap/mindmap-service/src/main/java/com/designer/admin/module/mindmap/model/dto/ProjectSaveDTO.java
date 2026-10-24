package com.designer.admin.module.mindmap.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 导图项目保存参数（新增 / 修改通用）。
 *
 * @author 雨夜终端
 */
@Data
public class ProjectSaveDTO {

    @ApiModelProperty("项目 ID，新增时留空")
    private Long id;

    @ApiModelProperty("分类 ID；新增时留空或 ≤0 表示未分类；修改时不传则保持原值")
    private Long categoryId;

    @ApiModelProperty("项目名称；为空时使用默认名")
    private String name;

    @ApiModelProperty("布局标识，如 logicalStructure")
    private String layout;

    @ApiModelProperty("主题标识")
    private String theme;

    @ApiModelProperty("simple-mind-map 节点树 JSON 字符串")
    private String content;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("为 true 时强制将分类置为未分类（优先于 categoryId）")
    private Boolean clearCategory;
}
