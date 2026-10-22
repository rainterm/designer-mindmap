package com.designer.admin.module.agent.util;

import org.noear.snack4.ONode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 模块 JSON 列读写助手（snack4）。
 *
 * @author 雨夜终端
 */
public final class AgentJson {

    private AgentJson() {
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return "[]";
        }
        return ONode.serialize(obj);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        Object obj = ONode.deserialize(json, List.class);
        if (obj instanceof List<?> list) {
            return new ArrayList<>((List<Object>) list);
        }
        return new ArrayList<>();
    }

    public static List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        List<Object> raw = parseArray(json);
        List<Long> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item == null) {
                continue;
            }
            if (item instanceof Number n) {
                result.add(n.longValue());
            } else {
                String s = String.valueOf(item).trim();
                if (!s.isEmpty()) {
                    result.add(Long.valueOf(s));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> parseMapList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        Object obj = ONode.deserialize(json, List.class);
        if (!(obj instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                map.forEach((k, v) -> row.put(String.valueOf(k), v));
                result.add(row);
            }
        }
        return result;
    }

    public static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        List<Object> raw = parseArray(json);
        List<String> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        Object obj = ONode.deserialize(json, Map.class);
        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Collections.emptyMap();
    }

    /** 反序列化为指定对象；空串/解析失败返回 null */
    public static <T> T parse(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return ONode.deserialize(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 节点 config 可能是 JSON 字符串，也可能已被反序列化为 Map/List。
     * 统一成可再解析的字符串，避免 Map#toString 丢失结构。
     */
    public static String configAsString(Object config) {
        if (config == null) {
            return "";
        }
        if (config instanceof String s) {
            return s;
        }
        if (config instanceof Map || config instanceof List) {
            return toJson(config);
        }
        return String.valueOf(config);
    }
}
