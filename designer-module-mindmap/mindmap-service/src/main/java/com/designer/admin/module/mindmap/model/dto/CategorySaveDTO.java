package com.designer.admin.module.mindmap.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.solon.validation.annotation.NotBlank;

/**
 * 导图分类保存参数（新增 / 修改通用）。
 *
 * @author 雨夜终端
 */
@Data
public class CategorySaveDTO {

    @ApiModelProperty("分类 ID，新增时留空")
    private Long id;

    @ApiModelProperty("父分类 ID，留空或 0 表示顶级")
    private Long parentId;

    @ApiModelProperty(value = "分类名称", required = true)
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @ApiModelProperty("排序值，越小越靠前；为空时自动追加到同级末尾")
    private Integer sort;
}
