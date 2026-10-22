package com.designer.admin.common.util;

import com.designer.admin.common.json.SensitiveSupport;
import org.noear.snack4.ONode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 序列化统一入口（基于 snack4，Solon 4.x 默认 JSON 引擎）
 *
 * @author 雨夜终端
 */
public final class JsonUtil {

    static {
        SensitiveSupport.register();
    }

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        return ONode.serialize(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return ONode.deserialize(json, clazz);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object obj = ONode.deserialize(json, Map.class);
        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Collections.emptyMap();
    }
}
