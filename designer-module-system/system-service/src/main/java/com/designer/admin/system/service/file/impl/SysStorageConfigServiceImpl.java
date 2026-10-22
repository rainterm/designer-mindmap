package com.designer.admin.system.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.crypto.ConfigSecretUtil;
import com.designer.admin.framework.spi.ConfigFieldMeta;
import com.designer.admin.framework.storage.FileStorageProvider;
import com.designer.admin.framework.storage.FileStorageProviderRegistry;
import com.designer.admin.framework.storage.ImageCompressor;
import com.designer.admin.framework.storage.StorageConfigSource;
import com.designer.admin.framework.storage.StorageProperties;
import com.designer.admin.framework.storage.StorageRuntimeConfig;
import com.designer.admin.framework.storage.provider.S3FileStorageProvider;
import com.designer.admin.system.constant.SysFileConsts;
import com.designer.admin.system.mapper.file.SysStorageConfigMapper;
import com.designer.admin.system.model.dto.file.StorageConfigQueryDTO;
import com.designer.admin.system.model.dto.file.StorageConfigSaveDTO;
import com.designer.admin.system.model.entity.file.SysStorageConfig;
import com.designer.admin.system.model.vo.file.StorageConfigDetailVO;
import com.designer.admin.system.model.vo.file.StorageOptionVO;
import com.designer.admin.system.model.vo.file.StorageTypeVO;
import com.designer.admin.system.service.file.SysStorageConfigService;
import com.designer.admin.system.util.ConfigFieldConverter;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件存储配置：CRUD + 默认配置缓存 + 运行时解析；实现 framework {@link StorageConfigSource}。
 *
 * @author 雨夜终端
 */
@Component
public class SysStorageConfigServiceImpl implements SysStorageConfigService, StorageConfigSource {

    @Db
    private SysStorageConfigMapper configMapper;

    @Inject
    private FileStorageProviderRegistry providerRegistry;

    @Inject
    private S3FileStorageProvider s3Provider;

    @Inject
    private StorageProperties storageProperties;

    /** 默认配置缓存 */
    private volatile StorageRuntimeConfig cachedDefault;

    @Override
    public IPage<SysStorageConfig> listPage(StorageConfigQueryDTO query) {
        IPage<SysStorageConfig> page = configMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysStorageConfig>()
                .like(notBlank(query.getConfigName()), SysStorageConfig::getConfigName, query.getConfigName())
                .eq(notBlank(query.getStorageType()), SysStorageConfig::getStorageType, query.getStorageType())
                .eq(query.getStatus() != null, SysStorageConfig::getStatus, query.getStatus())
                .eq(query.getIsPublic() != null, SysStorageConfig::getIsPublic, query.getIsPublic())
                .orderByDesc(SysStorageConfig::getIsDefault)
                .orderByDesc(SysStorageConfig::getId));
        page.getRecords().forEach(this::maskSecrets);
        return page;
    }

    @Override
    public SysStorageConfig getById(Long id) {
        SysStorageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("存储配置不存在");
        }
        return maskSecrets(cfg);
    }

    @Override
    public StorageConfigDetailVO getDetailView(Long id) {
        SysStorageConfig cfg = getById(id);
        return StorageConfigDetailVO.builder()
                .id(cfg.getId())
                .configName(cfg.getConfigName())
                .storageType(cfg.getStorageType())
                .isDefault(cfg.getIsDefault())
                .status(cfg.getStatus())
                .isPublic(cfg.getIsPublic())
                .urlPrefix(cfg.getUrlPrefix())
                .allowedExtensions(cfg.getAllowedExtensions())
                .maxSize(cfg.getMaxSize())
                .compressEnabled(cfg.getCompressEnabled())
                .compressQuality(cfg.getCompressQuality())
                .remark(cfg.getRemark())
                // 键集合由存储类型决定，只能是动态 map
                .config(JsonUtil.toMap(cfg.getConfigJson()))
                .createTime(cfg.getCreateTime())
                .updateTime(cfg.getUpdateTime())
                .build();
    }

    @Override
    public List<StorageTypeVO> listTypes() {
        String privateUrlPrefix = storageProperties.normalizedPrivateUrlPrefix();
        String publicUrlPrefix = storageProperties.normalizedPublicUrlPrefix();
        return providerRegistry.list().stream().map(p -> {
            StorageTypeVO vo = new StorageTypeVO();
            vo.setType(p.type());
            vo.setLabel(p.label());
            vo.setFields(p.configFields().stream().map(ConfigFieldConverter::toVO).toList());
            vo.setDefaultUrlPrefix(privateUrlPrefix);
            vo.setPublicUrlPrefix(publicUrlPrefix);
            return vo;
        }).toList();
    }

    @Override
    @Transaction
    public void add(StorageConfigSaveDTO dto) {
        validateSave(dto, true);
        FileStorageProvider provider = providerRegistry.getRequired(dto.getStorageType());
        Map<String, Object> props = dto.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getConfig());
        validateRequiredFields(provider, props);
        provider.validateConfig(props);

        SysStorageConfig entity = new SysStorageConfig();
        fillEntity(entity, dto, props);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlag();
        } else if (countEnabled() == 0) {
            entity.setIsDefault(1);
        }
        configMapper.insert(entity);
        clearCache();
    }

    @Override
    @Transaction
    public void update(StorageConfigSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("配置 ID 不能为空");
        }
        SysStorageConfig existing = configMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("存储配置不存在");
        }
        validateSave(dto, false);
        FileStorageProvider provider = providerRegistry.getRequired(dto.getStorageType());

        Map<String, Object> props = mergeConfig(existing, dto);
        validateRequiredFields(provider, props);
        provider.validateConfig(props);

        // 旧客户端未传访问类型时保持原值，避免默认值把公开配置误改成私有
        if (dto.getIsPublic() == null) {
            dto.setIsPublic(existing.getIsPublic() == null ? 0 : existing.getIsPublic());
        }
        // 压缩参数同理：老版本前端不会带这两个字段，不能因此把关掉的开关重新打开或把质量重置
        if (dto.getCompressEnabled() == null) {
            dto.setCompressEnabled(existing.getCompressEnabled());
        }
        if (dto.getCompressQuality() == null) {
            dto.setCompressQuality(existing.getCompressQuality());
        }

        SysStorageConfig entity = new SysStorageConfig();
        entity.setId(dto.getId());
        fillEntity(entity, dto, props);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        configMapper.updateById(entity);
        evictProviderClient(dto.getId());
        clearCache();
    }

    @Override
    @Transaction
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的配置");
        }
        Long defaultId = findDefaultId();
        for (Long id : ids) {
            if (defaultId != null && defaultId.equals(id)) {
                throw new BizException("不能删除默认存储配置，请先切换默认项");
            }
            configMapper.deleteById(id);
            evictProviderClient(id);
        }
        clearCache();
    }

    @Override
    @Transaction
    public void updateDefault(Long id) {
        SysStorageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("存储配置不存在");
        }
        if (cfg.getStatus() == null || cfg.getStatus() != 1) {
            throw new BizException("停用的配置不能设为默认");
        }
        clearDefaultFlag();
        SysStorageConfig upd = new SysStorageConfig();
        upd.setId(id);
        upd.setIsDefault(1);
        configMapper.updateById(upd);
        clearCache();
    }

    @Override
    @Transaction
    public void updateStatus(Long id, Integer status) {
        SysStorageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("存储配置不存在");
        }
        if (status != null && status == 0 && cfg.getIsDefault() != null && cfg.getIsDefault() == 1) {
            throw new BizException("默认配置不能停用，请先切换默认项");
        }
        SysStorageConfig upd = new SysStorageConfig();
        upd.setId(id);
        upd.setStatus(status);
        configMapper.updateById(upd);
        clearCache();
    }

    @Override
    public StorageRuntimeConfig requireDefaultRuntime() {
        StorageRuntimeConfig cached = cachedDefault;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedDefault != null) {
                return cachedDefault;
            }
            SysStorageConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                    .eq(SysStorageConfig::getIsDefault, 1)
                    .eq(SysStorageConfig::getStatus, 1)
                    .last("LIMIT 1"));
            if (cfg == null) {
                cfg = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                        .eq(SysStorageConfig::getStatus, 1)
                        .orderByAsc(SysStorageConfig::getId)
                        .last("LIMIT 1"));
            }
            if (cfg == null) {
                throw new BizException("未配置可用的文件存储，请先在「存储配置」中添加并启用");
            }
            cachedDefault = toRuntime(cfg);
            return cachedDefault;
        }
    }

    @Override
    public StorageRuntimeConfig requireRuntimeById(Long configId) {
        if (configId == null) {
            return requireDefaultRuntime();
        }
        SysStorageConfig cfg = configMapper.selectById(configId);
        if (cfg == null) {
            throw new BizException("文件对应的存储配置不存在");
        }
        return toRuntime(cfg);
    }

    @Override
    public List<StorageOptionVO> listPublicOptions() {
        List<SysStorageConfig> list = configMapper.selectList(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getStatus, 1)
                .eq(SysStorageConfig::getIsPublic, 1)
                .orderByDesc(SysStorageConfig::getIsDefault)
                .orderByAsc(SysStorageConfig::getId));
        return list.stream().map(cfg -> {
            StorageOptionVO vo = new StorageOptionVO();
            vo.setId(cfg.getId());
            vo.setConfigName(cfg.getConfigName());
            vo.setStorageType(cfg.getStorageType());
            vo.setIsDefault(cfg.getIsDefault());
            vo.setMaxSize(cfg.getMaxSize());
            return vo;
        }).toList();
    }

    @Override
    public StorageRuntimeConfig requireUploadRuntime(Long configId, boolean requirePublic) {
        StorageRuntimeConfig target;
        if (configId == null) {
            target = requireDefaultRuntime();
        } else {
            SysStorageConfig cfg = configMapper.selectById(configId);
            if (cfg == null) {
                throw new BizException("存储配置不存在");
            }
            if (cfg.getStatus() == null || cfg.getStatus() != 1) {
                throw new BizException("存储配置「" + cfg.getConfigName() + "」已停用，请先启用");
            }
            target = toRuntime(cfg);
        }
        if (requirePublic && !target.isPublicAccess()) {
            throw new BizException("该存储配置为私有，前台匿名访问的资源请选择公开配置");
        }
        return target;
    }

    @Override
    public StorageRuntimeConfig toRuntime(SysStorageConfig cfg) {
        boolean publicAccess = cfg.getIsPublic() != null && cfg.getIsPublic() == 1;
        // 库里存的是密文，交给 Provider 的必须是明文（S3 / SMTP 客户端不会自己解 ENC(...)）
        Map<String, Object> props = JsonUtil.toMap(cfg.getConfigJson());
        ConfigSecretUtil.decrypt(props, secretFieldNames(cfg.getStorageType()));
        return StorageRuntimeConfig.builder()
                .configId(cfg.getId())
                .configName(cfg.getConfigName())
                .storageType(cfg.getStorageType())
                // 前缀由访问类型决定，避免私有配置误配成公开前缀绕过鉴权
                .urlPrefix(storageProperties.urlPrefix(publicAccess))
                .publicAccess(publicAccess)
                .allowedExtensions(cfg.getAllowedExtensions())
                .maxSize(cfg.getMaxSize() == null ? 5242880L : cfg.getMaxSize())
                .compressEnabled(cfg.getCompressEnabled() != null && cfg.getCompressEnabled() == 1)
                .compressQuality(ImageCompressor.normalizeQuality(cfg.getCompressQuality()))
                .props(props)
                .build();
    }

    @Override
    public void clearCache() {
        cachedDefault = null;
    }

    private void fillEntity(SysStorageConfig entity, StorageConfigSaveDTO dto, Map<String, Object> props) {
        entity.setConfigName(dto.getConfigName().trim());
        entity.setStorageType(dto.getStorageType().trim());
        entity.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setIsPublic(dto.getIsPublic() == null ? 0 : dto.getIsPublic());
        entity.setUrlPrefix(storageProperties.urlPrefix(entity.getIsPublic() == 1));
        entity.setAllowedExtensions(blankToDefault(dto.getAllowedExtensions(),
                "jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,txt,md,markdown"));
        entity.setMaxSize(dto.getMaxSize() == null || dto.getMaxSize() <= 0 ? 5242880L : dto.getMaxSize());
        entity.setCompressEnabled(dto.getCompressEnabled() != null && dto.getCompressEnabled() == 1 ? 1 : 0);
        entity.setCompressQuality(ImageCompressor.normalizeQuality(dto.getCompressQuality()));
        // 密钥字段落库前加密（未配置 solon.vault.password 时为空操作）
        ConfigSecretUtil.encrypt(props, secretFieldNames(entity.getStorageType()));
        entity.setConfigJson(JsonUtil.toJson(props));
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
    }

    private Map<String, Object> mergeConfig(SysStorageConfig existing, StorageConfigSaveDTO dto) {
        Map<String, Object> oldProps = JsonUtil.toMap(existing.getConfigJson());
        Map<String, Object> incoming = dto.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getConfig());
        Set<String> secretFields = secretFieldNames(dto.getStorageType());
        for (String key : secretFields) {
            Object v = incoming.get(key);
            if (v == null || String.valueOf(v).isBlank() || SysFileConsts.SECRET_MASK.equals(String.valueOf(v))) {
                if (oldProps.containsKey(key)) {
                    incoming.put(key, oldProps.get(key));
                }
            }
        }
        return incoming;
    }

    private SysStorageConfig maskSecrets(SysStorageConfig cfg) {
        Map<String, Object> props = JsonUtil.toMap(cfg.getConfigJson());
        Set<String> secrets = secretFieldNames(cfg.getStorageType());
        boolean changed = false;
        for (String key : secrets) {
            Object v = props.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                props.put(key, SysFileConsts.SECRET_MASK);
                changed = true;
            }
        }
        if (changed) {
            cfg.setConfigJson(JsonUtil.toJson(props));
        }
        return cfg;
    }

    private Set<String> secretFieldNames(String storageType) {
        if (!providerRegistry.supports(storageType)) {
            return Set.of();
        }
        return providerRegistry.getRequired(storageType).configFields().stream()
                .filter(f -> "password".equalsIgnoreCase(f.getInputType()))
                .map(ConfigFieldMeta::getName)
                .collect(Collectors.toSet());
    }

    private void validateSave(StorageConfigSaveDTO dto, boolean creating) {
        if (!notBlank(dto.getConfigName())) {
            throw new BizException("配置名称不能为空");
        }
        if (!notBlank(dto.getStorageType())) {
            throw new BizException("存储类型不能为空");
        }
        if (dto.getIsPublic() != null && dto.getIsPublic() != 0 && dto.getIsPublic() != 1) {
            throw new BizException("访问类型只能是公开或私有");
        }
        if (!providerRegistry.supports(dto.getStorageType())) {
            throw new BizException("不支持的存储类型：" + dto.getStorageType());
        }
        if (creating && dto.getStatus() != null && dto.getStatus() == 0
                && dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            throw new BizException("停用配置不能设为默认");
        }
    }

    /** 按 Provider 字段元数据校验类型专属必填项 */
    private void validateRequiredFields(FileStorageProvider provider, Map<String, Object> props) {
        for (ConfigFieldMeta field : provider.configFields()) {
            if (!field.isRequired() || "switch".equalsIgnoreCase(field.getInputType())) {
                continue;
            }
            Object raw = props == null ? null : props.get(field.getName());
            if (raw == null || String.valueOf(raw).isBlank() || SysFileConsts.SECRET_MASK.equals(String.valueOf(raw))) {
                throw new BizException(field.getLabel() + "不能为空");
            }
        }
    }

    private void clearDefaultFlag() {
        configMapper.update(null, new LambdaUpdateWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getIsDefault, 1)
                .set(SysStorageConfig::getIsDefault, 0));
    }

    private long countEnabled() {
        return configMapper.selectCount(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getStatus, 1));
    }

    private Long findDefaultId() {
        SysStorageConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getIsDefault, 1)
                .last("LIMIT 1"));
        return cfg == null ? null : cfg.getId();
    }

    private void evictProviderClient(Long configId) {
        if (s3Provider != null) {
            s3Provider.evictClient(configId);
        }
    }


    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return notBlank(value) ? value.trim() : defaultValue;
    }
}
