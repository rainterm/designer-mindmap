package com.designer.admin.system.service.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.crypto.ConfigSecretUtil;
import com.designer.admin.framework.message.MessageChannelConsts;
import com.designer.admin.framework.message.MessageChannelProvider;
import com.designer.admin.framework.message.MessageChannelRegistry;
import com.designer.admin.framework.message.MessageConfigSource;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.framework.message.MessageService;
import com.designer.admin.framework.spi.ConfigFieldMeta;
import com.designer.admin.system.constant.SysMessageConsts;
import com.designer.admin.system.mapper.message.SysMessageConfigMapper;
import com.designer.admin.system.model.dto.message.MessageConfigQueryDTO;
import com.designer.admin.system.model.dto.message.MessageConfigSaveDTO;
import com.designer.admin.system.model.dto.message.MessageSendDTO;
import com.designer.admin.system.model.entity.message.SysMessageConfig;
import com.designer.admin.system.model.vo.message.MessageChannelTypeVO;
import com.designer.admin.system.model.vo.message.MessageConfigDetailVO;
import com.designer.admin.system.service.message.SysMessageConfigService;
import com.designer.admin.system.util.ConfigFieldConverter;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 消息通道配置：CRUD + 按通道类型的默认配置 + 测试发送；实现 framework {@link MessageConfigSource}。
 *
 * @author 雨夜终端
 */
@Component
public class SysMessageConfigServiceImpl implements SysMessageConfigService, MessageConfigSource {

    @Db
    private SysMessageConfigMapper configMapper;

    @Inject
    private MessageChannelRegistry channelRegistry;

    @Inject
    private MessageService messageService;

    /** channelType -> 默认配置缓存 */
    private final ConcurrentHashMap<String, MessageRuntimeConfig> defaultCache = new ConcurrentHashMap<>();

    @Override
    public IPage<SysMessageConfig> listPage(MessageConfigQueryDTO query) {
        IPage<SysMessageConfig> page = configMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysMessageConfig>()
                .like(notBlank(query.getConfigName()), SysMessageConfig::getConfigName, query.getConfigName())
                .eq(notBlank(query.getChannelType()), SysMessageConfig::getChannelType, query.getChannelType())
                .eq(query.getStatus() != null, SysMessageConfig::getStatus, query.getStatus())
                .orderByAsc(SysMessageConfig::getChannelType)
                .orderByDesc(SysMessageConfig::getIsDefault)
                .orderByDesc(SysMessageConfig::getId));
        page.getRecords().forEach(this::maskSecrets);
        return page;
    }

    @Override
    public MessageConfigDetailVO getDetailView(Long id) {
        SysMessageConfig cfg = getRawById(id);
        maskSecrets(cfg);
        return MessageConfigDetailVO.builder()
                .id(cfg.getId())
                .configName(cfg.getConfigName())
                .channelType(cfg.getChannelType())
                .isDefault(cfg.getIsDefault())
                .status(cfg.getStatus())
                .remark(cfg.getRemark())
                // 键集合由通道类型决定，只能是动态 map
                .config(JsonUtil.toMap(cfg.getConfigJson()))
                .createTime(cfg.getCreateTime())
                .updateTime(cfg.getUpdateTime())
                .build();
    }

    @Override
    public List<MessageChannelTypeVO> listTypes() {
        return channelRegistry.list().stream()
                // 站内通知置前，作为默认首选通道
                .sorted((a, b) -> {
                    if (MessageChannelConsts.TYPE_INBOX.equals(a.type())) {
                        return -1;
                    }
                    if (MessageChannelConsts.TYPE_INBOX.equals(b.type())) {
                        return 1;
                    }
                    return 0;
                })
                .map(p -> {
                    MessageChannelTypeVO vo = new MessageChannelTypeVO();
                    vo.setType(p.type());
                    vo.setLabel(p.label());
                    vo.setFields(p.configFields().stream().map(ConfigFieldConverter::toVO).toList());
                    return vo;
                }).toList();
    }

    @Override
    @Transaction
    public void add(MessageConfigSaveDTO dto) {
        validateSave(dto);
        MessageChannelProvider provider = channelRegistry.getRequired(dto.getChannelType());
        Map<String, Object> props = dto.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getConfig());
        provider.validateConfig(props);

        SysMessageConfig entity = new SysMessageConfig();
        fillEntity(entity, dto, props);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlag(dto.getChannelType());
        } else if (countEnabled(dto.getChannelType()) == 0) {
            entity.setIsDefault(1);
        }
        configMapper.insert(entity);
        clearCache(dto.getChannelType());
    }

    @Override
    @Transaction
    public void update(MessageConfigSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("配置 ID 不能为空");
        }
        SysMessageConfig existing = getRawById(dto.getId());
        validateSave(dto);
        MessageChannelProvider provider = channelRegistry.getRequired(dto.getChannelType());
        Map<String, Object> props = mergeConfig(existing, dto);
        provider.validateConfig(props);

        SysMessageConfig entity = new SysMessageConfig();
        entity.setId(dto.getId());
        fillEntity(entity, dto, props);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlag(dto.getChannelType());
        }
        configMapper.updateById(entity);
        clearCache(existing.getChannelType());
        clearCache(dto.getChannelType());
    }

    @Override
    @Transaction
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的配置");
        }
        for (Long id : ids) {
            SysMessageConfig cfg = getRawById(id);
            if (cfg.getIsDefault() != null && cfg.getIsDefault() == 1) {
                throw new BizException("不能删除默认通道配置，请先切换默认项：" + cfg.getConfigName());
            }
            configMapper.deleteById(id);
            clearCache(cfg.getChannelType());
        }
    }

    @Override
    @Transaction
    public void updateDefault(Long id) {
        SysMessageConfig cfg = getRawById(id);
        if (cfg.getStatus() == null || cfg.getStatus() != 1) {
            throw new BizException("停用的配置不能设为默认");
        }
        clearDefaultFlag(cfg.getChannelType());
        SysMessageConfig upd = new SysMessageConfig();
        upd.setId(id);
        upd.setIsDefault(1);
        configMapper.updateById(upd);
        clearCache(cfg.getChannelType());
    }

    @Override
    @Transaction
    public void updateStatus(Long id, Integer status) {
        SysMessageConfig cfg = getRawById(id);
        if (status != null && status == 0 && cfg.getIsDefault() != null && cfg.getIsDefault() == 1) {
            throw new BizException("默认配置不能停用，请先切换默认项");
        }
        SysMessageConfig upd = new SysMessageConfig();
        upd.setId(id);
        upd.setStatus(status);
        configMapper.updateById(upd);
        clearCache(cfg.getChannelType());
    }

    @Override
    public void testSend(MessageSendDTO dto) {
        if (dto == null || dto.getTo() == null || dto.getTo().isEmpty()) {
            throw new BizException("收件人不能为空");
        }
        MessagePayload payload = MessagePayload.builder()
                .to(dto.getTo())
                .cc(dto.getCc())
                .title(dto.getTitle() == null || dto.getTitle().isBlank() ? "测试消息" : dto.getTitle())
                .content(dto.getContent() == null || dto.getContent().isBlank() ? "这是一条测试消息" : dto.getContent())
                .html(Boolean.TRUE.equals(dto.getHtml()))
                .build();
        MessageRuntimeConfig runtime;
        if (dto.getConfigId() != null) {
            runtime = requireRuntimeById(dto.getConfigId());
        } else if (notBlank(dto.getChannelType())) {
            runtime = requireDefaultRuntime(dto.getChannelType());
        } else {
            throw new BizException("请指定配置或通道类型");
        }
        messageService.dispatch(runtime, payload);
    }

    @Override
    public MessageRuntimeConfig requireDefaultRuntime(String channelType) {
        if (!notBlank(channelType)) {
            throw new BizException("通道类型不能为空");
        }
        MessageRuntimeConfig cached = defaultCache.get(channelType);
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = defaultCache.get(channelType);
            if (cached != null) {
                return cached;
            }
            SysMessageConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysMessageConfig>()
                    .eq(SysMessageConfig::getChannelType, channelType)
                    .eq(SysMessageConfig::getIsDefault, 1)
                    .eq(SysMessageConfig::getStatus, 1)
                    .last("LIMIT 1"));
            if (cfg == null) {
                cfg = configMapper.selectOne(new LambdaQueryWrapper<SysMessageConfig>()
                        .eq(SysMessageConfig::getChannelType, channelType)
                        .eq(SysMessageConfig::getStatus, 1)
                        .orderByAsc(SysMessageConfig::getId)
                        .last("LIMIT 1"));
            }
            if (cfg == null) {
                throw new BizException("未配置可用的「" + channelType + "」消息通道，请先在通道配置中添加并启用");
            }
            MessageRuntimeConfig runtime = toRuntime(cfg);
            defaultCache.put(channelType, runtime);
            return runtime;
        }
    }

    @Override
    public MessageRuntimeConfig requireRuntimeById(Long configId) {
        return toRuntime(getRawById(configId));
    }

    @Override
    public MessageRuntimeConfig toRuntime(SysMessageConfig cfg) {
        // 库里存的是密文，交给 Provider 的必须是明文（SMTP 客户端不会自己解 ENC(...)）
        Map<String, Object> props = JsonUtil.toMap(cfg.getConfigJson());
        ConfigSecretUtil.decrypt(props, secretFieldNames(cfg.getChannelType()));
        return MessageRuntimeConfig.builder()
                .configId(cfg.getId())
                .configName(cfg.getConfigName())
                .channelType(cfg.getChannelType())
                .props(props)
                .build();
    }

    @Override
    public void clearCache(String channelType) {
        if (channelType != null) {
            defaultCache.remove(channelType);
        }
    }

    private SysMessageConfig getRawById(Long id) {
        SysMessageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("消息通道配置不存在");
        }
        return cfg;
    }

    private void fillEntity(SysMessageConfig entity, MessageConfigSaveDTO dto, Map<String, Object> props) {
        entity.setConfigName(dto.getConfigName().trim());
        entity.setChannelType(dto.getChannelType().trim());
        entity.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        // 密钥字段落库前加密（未配置 solon.vault.password 时为空操作）
        ConfigSecretUtil.encrypt(props, secretFieldNames(entity.getChannelType()));
        entity.setConfigJson(JsonUtil.toJson(props));
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
    }

    private Map<String, Object> mergeConfig(SysMessageConfig existing, MessageConfigSaveDTO dto) {
        Map<String, Object> oldProps = JsonUtil.toMap(existing.getConfigJson());
        Map<String, Object> incoming = dto.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getConfig());
        Set<String> secretFields = secretFieldNames(dto.getChannelType());
        for (String key : secretFields) {
            Object v = incoming.get(key);
            if (v == null || String.valueOf(v).isBlank() || SysMessageConsts.SECRET_MASK.equals(String.valueOf(v))) {
                if (oldProps.containsKey(key)) {
                    incoming.put(key, oldProps.get(key));
                }
            }
        }
        return incoming;
    }

    private SysMessageConfig maskSecrets(SysMessageConfig cfg) {
        Map<String, Object> props = JsonUtil.toMap(cfg.getConfigJson());
        Set<String> secrets = secretFieldNames(cfg.getChannelType());
        boolean changed = false;
        for (String key : secrets) {
            Object v = props.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                props.put(key, SysMessageConsts.SECRET_MASK);
                changed = true;
            }
        }
        if (changed) {
            cfg.setConfigJson(JsonUtil.toJson(props));
        }
        return cfg;
    }

    private Set<String> secretFieldNames(String channelType) {
        if (!channelRegistry.supports(channelType)) {
            return Set.of();
        }
        return channelRegistry.getRequired(channelType).configFields().stream()
                .filter(f -> "password".equalsIgnoreCase(f.getInputType()))
                .map(ConfigFieldMeta::getName)
                .collect(Collectors.toSet());
    }

    private void validateSave(MessageConfigSaveDTO dto) {
        if (!notBlank(dto.getConfigName())) {
            throw new BizException("配置名称不能为空");
        }
        if (!notBlank(dto.getChannelType())) {
            throw new BizException("通道类型不能为空");
        }
        if (!channelRegistry.supports(dto.getChannelType())) {
            throw new BizException("不支持的通道类型：" + dto.getChannelType());
        }
    }

    private void clearDefaultFlag(String channelType) {
        configMapper.update(null, new LambdaUpdateWrapper<SysMessageConfig>()
                .eq(SysMessageConfig::getChannelType, channelType)
                .eq(SysMessageConfig::getIsDefault, 1)
                .set(SysMessageConfig::getIsDefault, 0));
    }

    private long countEnabled(String channelType) {
        return configMapper.selectCount(new LambdaQueryWrapper<SysMessageConfig>()
                .eq(SysMessageConfig::getChannelType, channelType)
                .eq(SysMessageConfig::getStatus, 1));
    }


    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
