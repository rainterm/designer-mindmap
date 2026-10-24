package com.designer.admin.module.mindmap.api;

import com.designer.admin.module.mindmap.api.model.MmProjectBriefVO;

/**
 * 思维导图项目对外 API（供智能体等模块调用）。
 *
 * @author 雨夜终端
 */
public interface MmProjectApi {

    /**
     * 根据 Markdown 大纲新建导图项目。
     *
     * @param name       项目名称；空则使用默认名
     * @param outlineMd  Markdown 标题大纲（# / ## / ###）
     * @param categoryId 分类 ID，可空
     * @return 新建项目摘要
     */
    MmProjectBriefVO createFromOutline(String name, String outlineMd, Long categoryId);

    /**
     * 根据 Markdown 大纲覆盖更新导图内容。
     *
     * @param projectId 项目 ID
     * @param outlineMd Markdown 标题大纲
     * @param name      可选新名称；空则不改名
     * @return 更新后的项目摘要
     */
    MmProjectBriefVO updateFromOutline(Long projectId, String outlineMd, String name);
}
