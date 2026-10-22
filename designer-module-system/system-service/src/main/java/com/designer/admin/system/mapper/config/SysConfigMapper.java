package com.designer.admin.system.mapper.config;

import com.designer.admin.system.model.entity.config.SysConfig;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;

/**
 * 参数配置 Mapper
 *
 * @author 雨夜终端
 */
public interface SysConfigMapper extends MPJBaseMapper<SysConfig> {

    /**
     * 统计参数键名占用数（只统计未删除的配置）。
     *
     * <p>键名唯一性由应用层保证（表上无唯一索引），逻辑删除的键名不再占位，
     * 删除后可以用同一个键名重新配置。</p>
     *
     * @param configKey 参数键名
     * @param excludeId 需要排除的配置 ID，新增时传 {@code null}
     * @return 占用该键名的记录数
     */
    default Long countByConfigKey(String configKey, Long excludeId) {
        return selectJoinCount(JoinWrappers.lambda(SysConfig.class)
                .eq(SysConfig::getConfigKey, configKey)
                .ne(excludeId != null, SysConfig::getId, excludeId));
    }
}
