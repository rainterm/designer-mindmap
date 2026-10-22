package com.designer.admin.framework.message;

import com.designer.admin.common.util.PropUtil;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 消息通道运行时配置（来自 sys_message_config）
 *
 * <p>配置项来自库表 JSON，读取统一交给 {@link PropUtil}，
 * 与文件存储的运行时配置共用同一套容错口径。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class MessageRuntimeConfig {

    private Long configId;
    private String configName;
    private String channelType;
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

    public int propInt(String key, int defaultValue) {
        return PropUtil.getInt(props, key, defaultValue);
    }
}
