package com.designer.admin.system.service.config.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.constant.SysConfigConsts;
import com.designer.admin.system.constant.SystemRedisKeyConsts;
import com.designer.admin.system.mapper.config.SysConfigMapper;
import com.designer.admin.system.model.dto.config.ConfigQueryDTO;
import com.designer.admin.system.model.dto.config.ConfigSaveDTO;
import com.designer.admin.system.model.entity.config.SysConfig;
import com.designer.admin.system.service.config.SysConfigService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.List;

/**
 * 参数配置：CRUD + Redis 缓存按键取值
 *
 * @author 雨夜终端
 */
@Component
public class SysConfigServiceImpl implements SysConfigService {

    @Db
    private SysConfigMapper configMapper;
    @Inject
    private RedisClient redisClient;

    @Override
    public IPage<SysConfig> listPage(ConfigQueryDTO query) {
        return configMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysConfig>()
                .like(notBlank(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
                .like(notBlank(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                .eq(notBlank(query.getConfigType()), SysConfig::getConfigType, query.getConfigType())
                .orderByAsc(SysConfig::getId));
    }

    @Override
    public SysConfig getById(Long id) {
        SysConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("参数不存在");
        }
        return cfg;
    }

    @Override
    public String getValue(String configKey) {
        if (!notBlank(configKey)) {
            return null;
        }
        String cacheKey = SystemRedisKeyConsts.CONFIG_CACHE_PREFIX + configKey;
        String cached = redisClient.openAndGet(s -> s.key(cacheKey).get());
        if (cached != null) {
            return cached;
        }
        SysConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .last("limit 1"));
        String value = cfg == null ? null : cfg.getConfigValue();
        if (value != null) {
            redisClient.open(s -> s.key(cacheKey).expire(SysConfigConsts.CACHE_SECONDS).set(value));
        }
        return value;
    }

    @Override
    public boolean getBool(String configKey, boolean defaultValue) {
        String v = getValue(configKey);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim()) || "Y".equalsIgnoreCase(v.trim());
    }

    @Override
    public int getInt(String configKey, int defaultValue) {
        String v = getValue(configKey);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public long getTokenTimeoutSeconds() {
        int seconds = getInt(SysConfigConsts.KEY_TOKEN_TIMEOUT, SysConfigConsts.DEFAULT_TOKEN_TIMEOUT);
        if (seconds < SysConfigConsts.MIN_TOKEN_SECONDS) {
            return SysConfigConsts.DEFAULT_TOKEN_TIMEOUT;
        }
        return seconds;
    }

    @Override
    public long getTokenActiveTimeoutSeconds() {
        String v = getValue(SysConfigConsts.KEY_TOKEN_ACTIVE_TIMEOUT);
        if (v == null || v.isBlank()) {
            return SysConfigConsts.DEFAULT_TOKEN_ACTIVE_TIMEOUT;
        }
        try {
            int seconds = Integer.parseInt(v.trim());
            if (seconds == -1) {
                return -1;
            }
            if (seconds < SysConfigConsts.MIN_TOKEN_SECONDS) {
                return SysConfigConsts.DEFAULT_TOKEN_ACTIVE_TIMEOUT;
            }
            return seconds;
        } catch (NumberFormatException e) {
            return SysConfigConsts.DEFAULT_TOKEN_ACTIVE_TIMEOUT;
        }
    }

    @Override
    public void add(ConfigSaveDTO dto) {
        assertKeyUnique(dto.getConfigKey(), null);
        SysConfig cfg = toEntity(dto);
        if (!notBlank(cfg.getConfigType())) {
            cfg.setConfigType(SysConfigConsts.TYPE_NORMAL);
        }
        configMapper.insert(cfg);
        clearCache(cfg.getConfigKey());
    }

    @Override
    public void update(ConfigSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("参数ID不能为空");
        }
        SysConfig exist = getById(dto.getId());
        assertKeyUnique(dto.getConfigKey(), dto.getId());
        SysConfig cfg = toEntity(dto);
        // 内置标记不允许被改成可删
        if (SysConfigConsts.TYPE_BUILTIN.equalsIgnoreCase(exist.getConfigType())) {
            cfg.setConfigType(SysConfigConsts.TYPE_BUILTIN);
        } else if (!notBlank(cfg.getConfigType())) {
            cfg.setConfigType(SysConfigConsts.TYPE_NORMAL);
        }
        configMapper.updateById(cfg);
        clearCache(exist.getConfigKey());
        if (!exist.getConfigKey().equals(cfg.getConfigKey())) {
            clearCache(cfg.getConfigKey());
        }
    }

    @Override
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<SysConfig> list = configMapper.selectList(new LambdaQueryWrapper<SysConfig>().in(SysConfig::getId, ids));
        for (SysConfig cfg : list) {
            if (SysConfigConsts.TYPE_BUILTIN.equalsIgnoreCase(cfg.getConfigType())) {
                throw new BizException("内置参数「" + cfg.getConfigKey() + "」不允许删除");
            }
        }
        configMapper.deleteByIds(ids);
        for (SysConfig cfg : list) {
            clearCache(cfg.getConfigKey());
        }
    }

    @Override
    public void clearCache(String configKey) {
        if (!notBlank(configKey)) {
            return;
        }
        redisClient.open(s -> s.key(SystemRedisKeyConsts.CONFIG_CACHE_PREFIX + configKey).delete());
    }

    @Override
    public void refreshCache() {
        List<SysConfig> all = configMapper.selectList(null);
        for (SysConfig cfg : all) {
            clearCache(cfg.getConfigKey());
        }
    }

    private void assertKeyUnique(String key, Long excludeId) {
        Long cnt = configMapper.countByConfigKey(key, excludeId);
        if (cnt != null && cnt > 0) {
            throw new BizException("参数键名已存在");
        }
    }

    private SysConfig toEntity(ConfigSaveDTO dto) {
        SysConfig cfg = new SysConfig();
        cfg.setId(dto.getId());
        cfg.setConfigName(dto.getConfigName());
        cfg.setConfigKey(dto.getConfigKey());
        cfg.setConfigValue(dto.getConfigValue());
        cfg.setConfigType(dto.getConfigType());
        cfg.setRemark(dto.getRemark());
        return cfg;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
