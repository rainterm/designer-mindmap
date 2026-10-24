package com.designer.admin.module.mindmap.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.module.mindmap.model.dto.ProjectQueryDTO;
import com.designer.admin.module.mindmap.model.dto.ProjectSaveDTO;
import com.designer.admin.module.mindmap.model.vo.ProjectVO;

import java.util.List;

/**
 * 思维导图项目服务。
 *
 * @author 雨夜终端
 */
public interface MmProjectService {

    /**
     * 分页查询导图项目（列表不含 content）。
     *
     * @param query 名称、分类等筛选条件
     * @return 项目分页
     */
    IPage<ProjectVO> listPage(ProjectQueryDTO query);

    /**
     * 按主键查询项目详情（含 content），不存在时抛出业务异常。
     *
     * @param id 项目 ID
     * @return 项目视图
     */
    ProjectVO getById(Long id);

    /**
     * 新增导图项目；名称为空时使用默认名，内容为空时使用默认根节点树。
     *
     * @param dto 保存参数
     * @return 新建后的项目视图（含 content）
     */
    ProjectVO add(ProjectSaveDTO dto);

    /**
     * 修改导图项目；可通过 clearCategory 或 categoryId 调整分类。
     *
     * @param dto 保存参数
     * @return 更新后的项目视图（含 content）
     */
    ProjectVO update(ProjectSaveDTO dto);

    /**
     * 批量删除导图项目（逻辑删除）。
     *
     * @param ids 项目 ID 列表
     */
    void remove(List<Long> ids);
}
