package com.designer.admin.system.service.auth;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.auth.OauthScopeQueryDTO;
import com.designer.admin.system.model.dto.auth.OauthScopeSaveDTO;
import com.designer.admin.system.model.entity.auth.SysOauthScope;

import java.util.List;

/**
 * OAuth2 Scope：CRUD，以及换票与路径鉴权所需的启用范围查询。
 *
 * @author 雨夜终端
 */
public interface SysOauthScopeService {

    /**
     * OAuth2 Scope 分页。
     *
     * @param query 分页与名称 / 编码筛选条件
     * @return Scope 分页结果
     */
    IPage<SysOauthScope> listPage(OauthScopeQueryDTO query);

    /**
     * 全部启用中的 Scope。
     *
     * @return Scope 列表
     */
    List<SysOauthScope> listEnabled();

    /**
     * 启用中的 Scope 匹配模式，按模式长度倒序（最长前缀优先）。
     *
     * @return 匹配模式列表
     */
    List<SysOauthScope> listAllEnabledPatterns();

    /**
     * 按主键查询 Scope，不存在时抛出业务异常。
     *
     * @param id Scope ID
     * @return Scope
     */
    SysOauthScope getById(Long id);

    /**
     * 新增 Scope。
     *
     * @param dto 保存参数
     * @return 新增的 Scope
     */
    SysOauthScope add(OauthScopeSaveDTO dto);

    /**
     * 修改 Scope。
     *
     * @param dto 保存参数
     */
    void update(OauthScopeSaveDTO dto);

    /**
     * 批量删除 Scope。
     *
     * @param ids Scope ID 列表
     */
    void remove(List<Long> ids);
}
