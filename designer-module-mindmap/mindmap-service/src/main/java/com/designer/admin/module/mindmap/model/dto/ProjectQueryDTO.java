package com.designer.admin.module.mindmap.model.dto;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导图项目分页查询参数。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQueryDTO extends PageQuery {

    @ApiModelProperty("项目名称，模糊匹配")
    private String name;

    @ApiModelProperty("分类 ID；0 或负数表示未分类；为空表示不限")
    private Long categoryId;
}
