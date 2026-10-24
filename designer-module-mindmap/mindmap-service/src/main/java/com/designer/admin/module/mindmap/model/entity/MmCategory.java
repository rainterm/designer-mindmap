package com.designer.admin.module.mindmap.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 思维导图分类（树形，最多 5 层）。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mm_category")
public class MmCategory extends BaseEntity {

    /** 父分类 ID，0 为顶级 */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 排序值，越小越靠前 */
    private Integer sort;
}
