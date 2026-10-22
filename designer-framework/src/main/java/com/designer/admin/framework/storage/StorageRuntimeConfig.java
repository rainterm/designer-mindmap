package com.designer.admin.framework.storage;

import com.designer.admin.common.util.PropUtil;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 一次存储操作所用的运行时配置（来自数据库 sys_storage_config）
 *
 * <p>配置项来自库表 JSON，读取统一交给 {@link PropUtil}，
 * 与消息通道的运行时配置共用同一套容错口径。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class StorageRuntimeConfig {

    private Long configId;
    private String configName;
    private String storageType;
    private String urlPrefix;
    private String allowedExtensions;
    private long maxSize;
    /** 是否压缩上传图片（只影响 jpg / png，其余类型直接跳过） */
    private boolean compressEnabled;
    /** JPEG 压缩质量百分比（10 ~ 100），越小体积越小；PNG 为无损压缩、忽略该值 */
    @Builder.Default
    private int compressQuality = ImageCompressor.DEFAULT_QUALITY;
    /** 是否公开访问：公开配置的 URL 走 /app-api，私有配置走 /admin-api */
    private boolean publicAccess;
    /** 类型专属配置（已解析的 JSON Map） */
    @Builder.Default
    private Map<String, Object> props = Collections.emptyMap();

    public String prop(String key) {
        return PropUtil.getString(props, key);
    }

    public String prop(String key, String defaultValue) {
        return PropUtil.getString(props, key, defaultValue);
    }

    public boolean propBool(String key, boolean defaultValue) {
        return PropUtil.getBool(props, key, defaultValue);
    }

    public long propLong(String key, long defaultValue) {
        return PropUtil.getLong(props, key, defaultValue);
    }
}
