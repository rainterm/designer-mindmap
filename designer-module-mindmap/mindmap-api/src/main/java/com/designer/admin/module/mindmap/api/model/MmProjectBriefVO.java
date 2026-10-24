package com.designer.admin.module.mindmap.api.model;

import lombok.Builder;
import lombok.Data;

/**
 * 思维导图项目摘要（跨模块契约出参）。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class MmProjectBriefVO {

    /** 项目 ID */
    private Long id;

    /** 项目名称 */
    private String name;

    /** 管理端编辑路径，如 /mindmap/project/edit?id=1 */
    private String editPath;
}
