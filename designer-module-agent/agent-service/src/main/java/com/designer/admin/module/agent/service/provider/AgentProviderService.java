package com.designer.admin.module.agent.service.provider;

import com.designer.admin.module.agent.model.dto.provider.ProviderFetchModelsDTO;
import com.designer.admin.module.agent.model.dto.provider.ProviderQueryDTO;
import com.designer.admin.module.agent.model.dto.provider.ProviderSaveDTO;
import com.designer.admin.module.agent.model.vo.provider.ProviderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;

/**
 * 模型供应商
 *
 * @author 雨夜终端
 */
public interface AgentProviderService {

    /**
     * 供应商分页。
     *
     * @param query 分页与名称 / 类型 / 状态筛选条件
     * @return 供应商分页结果
     */
    IPage<ProviderVO> page(ProviderQueryDTO query);

    /**
     * 已启用供应商列表（下拉等场景）。
     *
     * @return 已启用供应商列表
     */
    List<ProviderVO> listEnabled();

    /**
     * 按主键查询供应商详情。
     *
     * @param id 供应商 ID
     * @return 供应商详情
     */
    ProviderVO getById(Long id);

    /**
     * Solon 聊天方言预设（新建表单下拉）。
     *
     * @return 方言列表（code / label / defaultBaseUrl / hint）
     */
    List<Map<String, Object>> listVendors();

    /**
     * 新增供应商。
     *
     * @param dto 保存参数
     * @return 新建后的供应商
     */
    ProviderVO save(ProviderSaveDTO dto);

    /**
     * 修改供应商。
     *
     * @param dto 保存参数
     * @return 更新后的供应商
     */
    ProviderVO update(ProviderSaveDTO dto);

    /**
     * 批量删除供应商。
     *
     * @param ids 供应商 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 按 Base URL / API Key 拉取远端模型列表；编辑时可传 id 复用已存密钥。
     *
     * @param dto 拉取参数（类型、地址、密钥或供应商 ID）
     * @return 模型列表（id / name 等）
     */
    List<Map<String, Object>> fetchModels(ProviderFetchModelsDTO dto);

}
