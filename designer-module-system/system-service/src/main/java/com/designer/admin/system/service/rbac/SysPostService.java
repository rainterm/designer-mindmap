package com.designer.admin.system.service.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.rbac.PostQueryDTO;
import com.designer.admin.system.model.dto.rbac.PostSaveDTO;
import com.designer.admin.system.model.entity.rbac.SysPost;

import java.util.List;

/**
 * 岗位服务（全局字典；部门通过用户关联）
 *
 * @author 雨夜终端
 */
public interface SysPostService {

    /**
     * 岗位分页。
     *
     * @param query 分页与名称 / 编码 / 状态筛选条件
     * @return 岗位分页结果
     */
    IPage<SysPost> listPage(PostQueryDTO query);

    /**
     * 启用岗位下拉。
     *
     * @return 岗位列表
     */
    List<SysPost> listOptions();

    /**
     * 按主键查询岗位，不存在时抛出业务异常。
     *
     * @param id 岗位 ID
     * @return 岗位
     */
    SysPost getById(Long id);

    /**
     * 新增岗位，校验编码与名称唯一。
     *
     * @param dto 保存参数
     */
    void add(PostSaveDTO dto);

    /**
     * 修改岗位。
     *
     * @param dto 保存参数
     */
    void update(PostSaveDTO dto);

    /**
     * 批量删除岗位；已分配用户时拒绝。
     *
     * @param ids 岗位 ID 列表
     */
    void remove(List<Long> ids);
}
