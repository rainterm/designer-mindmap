package com.designer.admin.module.mindmap.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导图分类树节点。
 *
 * @author 雨夜终端
 */
@Data
public class CategoryVO {

    @ApiModelProperty("分类 ID")
    private Long id;

    @ApiModelProperty("父分类 ID，0 为顶级")
    private Long parentId;

    @ApiModelProperty("分类名称")
    private String name;

    @ApiModelProperty("排序值，越小越靠前")
    private Integer sort;

    @ApiModelProperty("本分类直接挂载的导图数")
    private Long projectCount;

    @ApiModelProperty("含子孙分类的导图总数")
    private Long totalCount;

    @ApiModelProperty("子分类")
    private List<CategoryVO> children = new ArrayList<>();
}
