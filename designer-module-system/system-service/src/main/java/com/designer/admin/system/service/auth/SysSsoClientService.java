package com.designer.admin.system.service.auth;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.auth.SsoClientQueryDTO;
import com.designer.admin.system.model.dto.auth.SsoClientSaveDTO;
import com.designer.admin.system.model.entity.auth.SysSsoClient;

import java.util.List;

/**
 * SSO 客户端：白名单维护与回跳校验
 *
 * @author 雨夜终端
 */
public interface SysSsoClientService {

    /**
     * SSO 客户端分页。
     *
     * @param query 分页与名称 / clientKey 筛选条件
     * @return 客户端分页结果
     */
    IPage<SysSsoClient> listPage(SsoClientQueryDTO query);

    /**
     * 全部启用中的 SSO 客户端。
     *
     * @return 客户端列表
     */
    List<SysSsoClient> listEnabled();

    /**
     * 按主键查询 SSO 客户端，不存在时抛出业务异常。
     *
     * @param id 客户端 ID
     * @return 客户端
     */
    SysSsoClient getById(Long id);

    /**
     * 按 clientKey 查询启用中的客户端（换票用）。
     *
     * @param clientKey 客户端标识
     * @return 客户端；不存在或已停用时返回 null
     */
    SysSsoClient getEnabledByKey(String clientKey);

    /**
     * 校验回跳地址的 Origin 是否在启用客户端白名单内，不允许时抛出业务异常。
     *
     * @param redirect 回跳地址
     */
    void assertAllowedReturnUrl(String redirect);

    /**
     * 新增 SSO 客户端，校验 clientKey 唯一。
     *
     * @param dto 保存参数
     */
    void add(SsoClientSaveDTO dto);

    /**
     * 修改 SSO 客户端。
     *
     * @param dto 保存参数
     */
    void update(SsoClientSaveDTO dto);

    /**
     * 批量删除 SSO 客户端。
     *
     * @param ids 客户端 ID 列表
     */
    void remove(List<Long> ids);
}
