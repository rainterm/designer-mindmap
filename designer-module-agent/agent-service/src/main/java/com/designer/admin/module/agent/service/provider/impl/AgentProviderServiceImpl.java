package com.designer.admin.module.agent.service.provider.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.model.dto.provider.ProviderFetchModelsDTO;
import com.designer.admin.module.agent.model.dto.provider.ProviderQueryDTO;
import com.designer.admin.module.agent.model.dto.provider.ProviderSaveDTO;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.model.vo.provider.ProviderVO;
import com.designer.admin.module.agent.service.provider.AgentProviderModelClient;
import com.designer.admin.module.agent.service.provider.AgentProviderService;
import com.designer.admin.module.agent.service.provider.AgentVendorPresets;
import com.designer.admin.module.agent.util.AgentEntitySupport;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.util.AgentTexts;

/**
 * 模型供应商
 *
 * @author 雨夜终端
 */
@Component
public class AgentProviderServiceImpl implements AgentProviderService {

    @Db
    private AgentProviderMapper providerMapper;
    @Inject
    private AgentProviderModelClient modelClient;

    @Override
    public IPage<ProviderVO> page(ProviderQueryDTO query) {
        List<String> typeKeys = AgentTexts.notBlank(query.getType())
                ? AgentVendorPresets.typesMatching(query.getType())
                : null;
        IPage<AgentProvider> page = providerMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentProvider>()
                .like(AgentTexts.notBlank(query.getName()), AgentProvider::getName, query.getName())
                .in(typeKeys != null, AgentProvider::getType, typeKeys)
                .eq(query.getStatus() != null, AgentProvider::getStatus, query.getStatus())
                .orderByDesc(AgentProvider::getId));
        return page.convert(this::toVo);
    }

    @Override
    public List<ProviderVO> listEnabled() {
        return providerMapper.selectList(new LambdaQueryWrapper<AgentProvider>()
                        .eq(AgentProvider::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByDesc(AgentProvider::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public ProviderVO getById(Long id) {
        return toVo(require(id));
    }

    @Override
    public List<Map<String, Object>> listVendors() {
        return AgentVendorPresets.all().stream().map(AgentVendorPresets::toMap).toList();
    }

    @Override
    public ProviderVO save(ProviderSaveDTO dto) {
        if (!AgentTexts.notBlank(dto.getName())) {
            throw new BizException("供应商名称不能为空");
        }
        AgentProvider entity = new AgentProvider();
        entity.setName(dto.getName().trim());
        entity.setType(AgentVendorPresets.toStandard(AgentTexts.notBlank(dto.getType()) ? dto.getType() : "openai"));
        entity.setBaseUrl(dto.getBaseUrl() == null ? "" : dto.getBaseUrl().trim());
        String apiKey = dto.getApiKey() == null ? "" : dto.getApiKey();
        entity.setApiKey(apiKey);
        entity.setApiKeyMasked(maskKey(apiKey));
        entity.setModelsJson(AgentJson.toJson(dto.getModels() == null ? List.of() : dto.getModels()));
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        providerMapper.insert(entity);
        return toVo(entity);
    }

    @Override
    public ProviderVO update(ProviderSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("供应商ID不能为空");
        }
        AgentProvider existing = require(dto.getId());
        if (!AgentTexts.notBlank(dto.getName())) {
            throw new BizException("供应商名称不能为空");
        }
        existing.setName(dto.getName().trim());
        existing.setType(AgentTexts.notBlank(dto.getType())
                ? AgentVendorPresets.toStandard(dto.getType())
                : AgentVendorPresets.toStandard(existing.getType()));
        existing.setBaseUrl(dto.getBaseUrl() == null ? "" : dto.getBaseUrl().trim());
        existing.setModelsJson(AgentJson.toJson(dto.getModels() == null ? List.of() : dto.getModels()));
        existing.setStatus(dto.getStatus());
        existing.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        if (AgentTexts.notBlank(dto.getApiKey())) {
            existing.setApiKey(dto.getApiKey());
            existing.setApiKeyMasked(maskKey(dto.getApiKey()));
        }
        providerMapper.updateById(existing);
        return toVo(existing);
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        AgentEntitySupport.deleteBatchIds(providerMapper, ids);
    }

    @Override
    public List<Map<String, Object>> fetchModels(ProviderFetchModelsDTO dto) {
        if (dto == null) {
            throw new BizException("参数不能为空");
        }
        String type = dto.getType();
        String baseUrl = dto.getBaseUrl();
        String apiKey = dto.getApiKey();
        List<Map<String, Object>> existing = List.of();

        if (dto.getId() != null) {
            AgentProvider row = require(dto.getId());
            if (!AgentTexts.notBlank(type)) {
                type = row.getType();
            }
            if (!AgentTexts.notBlank(baseUrl)) {
                baseUrl = row.getBaseUrl();
            }
            if (!AgentTexts.notBlank(apiKey)) {
                apiKey = row.getApiKey();
            }
            existing = AgentJson.parseMapList(row.getModelsJson());
        }

        List<Map<String, Object>> fetched = modelClient.fetchModels(
                AgentVendorPresets.toStandard(type), baseUrl, apiKey);
        return mergeModels(existing, fetched);
    }

    /**
     * 合并远端与本地模型：保留本地未乱码的 displayName / status；
     * contextWindow、modelType 仅在远端缺失时回退本地。
     */
    static List<Map<String, Object>> mergeModels(List<Map<String, Object>> existing,
                                                 List<Map<String, Object>> fetched) {
        Map<String, Map<String, Object>> oldByCode = existing == null ? Map.of() : existing.stream()
                .filter(Objects::nonNull)
                .filter(m -> m.get("modelCode") != null)
                .collect(Collectors.toMap(
                        m -> String.valueOf(m.get("modelCode")),
                        m -> m,
                        (a, b) -> a,
                        LinkedHashMap::new));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> item : fetched) {
            Map<String, Object> row = new LinkedHashMap<>(item);
            String code = String.valueOf(row.getOrDefault("modelCode", ""));
            Map<String, Object> old = oldByCode.get(code);
            if (old != null) {
                String oldName = old.get("displayName") == null ? "" : String.valueOf(old.get("displayName")).trim();
                if (!oldName.isBlank() && !isLikelyEncodingGarbage(oldName)) {
                    row.put("displayName", oldName);
                }
                // 远端未返回上下文时，才保留本地已填值
                if (row.get("contextWindow") == null && old.get("contextWindow") != null) {
                    row.put("contextWindow", old.get("contextWindow"));
                }
                // 远端未识别类型时，保留本地已选类型
                if (row.get("modelType") == null && old.get("modelType") != null) {
                    row.put("modelType", old.get("modelType"));
                }
                if (old.get("status") != null) {
                    row.put("status", old.get("status"));
                }
            }
            result.add(row);
        }
        return result;
    }

    /** 历史编码错误会把中文落成连续 '?'，重新拉取时应丢弃。 */
    static boolean isLikelyEncodingGarbage(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return true;
        }
        int q = 0;
        for (int i = 0; i < displayName.length(); i++) {
            if (displayName.charAt(i) == '?') {
                q++;
            }
        }
        return q >= 2;
    }

    private AgentProvider require(Long id) {
        return AgentEntitySupport.require(providerMapper.selectById(id), "供应商不存在");
    }

    private ProviderVO toVo(AgentProvider e) {
        ProviderVO vo = new ProviderVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setType(AgentVendorPresets.toStandard(e.getType()));
        vo.setBaseUrl(e.getBaseUrl());
        vo.setApiKey("");
        vo.setApiKeyMasked(e.getApiKeyMasked());
        vo.setModels(AgentJson.parseMapList(e.getModelsJson()));
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}
