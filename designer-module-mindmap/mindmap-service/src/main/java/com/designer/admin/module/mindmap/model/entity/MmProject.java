package com.designer.admin.module.mindmap.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 思维导图项目；内容以 simple-mind-map JSON 落库。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mm_project")
public class MmProject extends BaseEntity {

    /** 分类 ID，空表示未分类 */
    private Long categoryId;

    /** 项目名称 */
    private String name;

    /** 布局：logicalStructure / mindMap 等 */
    private String layout;

    /** 主题 key */
    private String theme;

    /** 导图节点树 JSON（simple-mind-map 格式） */
    private String content;

    /** 备注 */
    private String remark;
}
