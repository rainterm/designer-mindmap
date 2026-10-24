package com.designer.admin.module.mindmap.service;

import com.designer.admin.module.mindmap.model.dto.CategorySaveDTO;
import com.designer.admin.module.mindmap.model.entity.MmCategory;
import com.designer.admin.module.mindmap.model.vo.CategoryVO;

import java.util.List;

/**
 * 思维导图分类服务。
 *
 * @author 雨夜终端
 */
public interface MmCategoryService {

    /**
     * 查询分类树（含本级 / 含子级导图数量）。
     *
     * @return 顶级分类列表，子节点挂在 children
     */
    List<CategoryVO> listTree();

    /**
     * 查询全部扁平分类（供内部筛选与深度校验）。
     *
     * @return 按排序与 ID 升序的分类列表
     */
    List<MmCategory> listAll();

    /**
     * 统计未分类导图数量。
     *
     * @return 未挂分类的项目数
     */
    long countUncategorized();

    /**
     * 收集指定分类及其全部子孙分类 ID（含自身）。
     *
     * @param categoryId 分类 ID
     * @return ID 列表；非法入参时返回空列表
     */
    List<Long> listSelfAndDescendantIds(Long categoryId);

    /**
     * 新增分类；同级名称唯一，深度不超过 5 层。
     *
     * @param dto 保存参数
     * @return 新建后的分类视图
     */
    CategoryVO add(CategorySaveDTO dto);

    /**
     * 修改分类；禁止挂到自身或子孙下。
     *
     * @param dto 保存参数
     * @return 更新后的分类视图
     */
    CategoryVO update(CategorySaveDTO dto);

    /**
     * 删除分类：子分类上移至父级，本分类下导图改为未分类。
     *
     * @param id 分类 ID
     */
    void remove(Long id);

    /**
     * 按主键查询分类，不存在时抛出业务异常。
     *
     * @param id 分类 ID
     * @return 分类实体
     */
    MmCategory getById(Long id);
}
