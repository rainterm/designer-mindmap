package com.designer.admin.system.service.rbac;

import com.designer.admin.system.model.dto.rbac.DeptSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysDept;

import java.util.List;

/**
 * 部门树 CRUD
 *
 * @author 雨夜终端
 */
public interface SysDeptService {

    /**
     * 部门树查询。
     *
     * @param deptName 部门名称关键字，可空
     * @param status   状态过滤，可空
     * @return 部门树
     */
    List<SysDept> listTree(String deptName, Integer status);

    /**
     * 启用部门树（下拉选项用）。
     *
     * @return 部门树
     */
    List<SysDept> listOptions();

    /**
     * 按主键查询部门，不存在时抛出业务异常。
     *
     * @param id 部门 ID
     * @return 部门
     */
    SysDept getById(Long id);

    /**
     * 新增部门，并按 parentId 维护 ancestors 路径。
     *
     * @param dto 保存参数
     */
    void add(DeptSaveDTO dto);

    /**
     * 修改部门，父节点变化时同步刷新子孙节点的 ancestors。
     *
     * @param dto 保存参数
     */
    void update(DeptSaveDTO dto);

    /**
     * 删除部门，存在子部门时拒绝。
     *
     * @param id 部门 ID
     */
    void remove(Long id);
}
