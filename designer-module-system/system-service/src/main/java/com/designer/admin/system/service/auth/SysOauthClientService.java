package com.designer.admin.system.service.auth;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.auth.OauthClientQueryDTO;
import com.designer.admin.system.model.dto.auth.OauthClientSaveDTO;
import com.designer.admin.system.model.entity.auth.SysOauthClient;
import com.designer.admin.system.model.vo.auth.OauthClientCreatedVO;

import java.util.List;

/**
 * OAuth2 客户端：CRUD、密钥签发与重置、授权 Scope 维护。
 *
 * @author 雨夜终端
 */
public interface SysOauthClientService {

    /**
     * OAuth2 客户端分页。
     *
     * @param query 分页与名称 / clientId 筛选条件
     * @return 客户端分页结果
     */
    IPage<SysOauthClient> listPage(OauthClientQueryDTO query);

    /**
     * 按主键查询客户端，不存在时抛出业务异常。
     *
     * @param id 客户端 ID
     * @return 客户端
     */
    SysOauthClient getById(Long id);

    /**
     * 按 clientId 查询启用中的客户端（换票用）。
     *
     * @param clientId 客户端标识
     * @return 客户端；不存在或已停用时返回 null
     */
    SysOauthClient getEnabledByClientId(String clientId);

    /**
     * 查询客户端已授权的 Scope 编码列表。
     *
     * @param clientPk 客户端主键
     * @return Scope 编码列表
     */
    List<String> listAuthorizedScopeCodes(Long clientPk);

    /**
     * 新增客户端，返回明文密钥（仅此一次）。
     *
     * @param dto 保存参数
     * @return 创建结果（含明文密钥）
     */
    OauthClientCreatedVO add(OauthClientSaveDTO dto);

    /**
     * 修改客户端；密钥为空表示保持原值。
     *
     * @param dto 保存参数
     */
    void update(OauthClientSaveDTO dto);

    /**
     * 重置客户端密钥，返回新的明文密钥。
     *
     * @param id 客户端 ID
     * @return 新密钥
     */
    OauthClientCreatedVO resetSecret(Long id);

    /**
     * 批量删除客户端。
     *
     * @param ids 客户端 ID 列表
     */
    void remove(List<Long> ids);
}
