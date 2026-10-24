package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.util.AgentJsonUtil;
import org.noear.snack4.ONode;
import org.noear.solon.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点 config JSON 解析与步骤参数小工具。
 *
 * @author 雨夜终端
 */
final class WorkflowNodeConfigs {

    /**
     * 禁止实例化。
     */
    private WorkflowNodeConfigs() {
    }

    /**
     * 将节点配置 JSON 解析为指定 DTO；空白配置返回空实例。
     *
     * @param configJson 配置 JSON
     * @param clazz      DTO 类型
     * @param <T>        DTO 类型
     * @return 解析结果；解析失败返回 null（调用方应走 raw fallback）
     */
    static <T> T parse(String configJson, Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        if (Utils.isBlank(configJson)) {
            return newEmpty(clazz);
        }
        T parsed = AgentJsonUtil.parse(configJson.trim(), clazz);
        if (parsed != null) {
            return parsed;
        }
        // 非 JSON 对象（如纯文本模板）无法反序列化
        return null;
    }

    /**
     * 配置无法解析为 DTO 且原文非空时，退回原始字符串，便于 step 面板展示。
     *
     * @param configJson 原始配置
     * @param dto        已解析 DTO；null 表示解析失败
     * @return 需回退时返回含 {@code config} 键的 Map；否则 null
     */
    static Map<String, Object> rawConfigFallback(String configJson, Object dto) {
        if (dto != null) {
            return null;
        }
        if (Utils.isBlank(configJson)) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("config", configJson.trim());
        return out;
    }

    /**
     * 将 DTO 转为有序 Map，供 step.params 展示（字段名与 JSON 一致）。
     *
     * @param dto 配置 DTO
     * @return 参数 Map；dto 为 null 时返回空 Map
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> toParamMap(Object dto) {
        if (dto == null) {
            return Map.of();
        }
        try {
            Object obj = ONode.deserialize(ONode.serialize(dto), Map.class);
            if (obj instanceof Map<?, ?> map) {
                Map<String, Object> out = new LinkedHashMap<>();
                map.forEach((k, v) -> {
                    if (k != null) {
                        out.put(String.valueOf(k), v);
                    }
                });
                return out;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Map.of();
    }

    private static <T> T newEmpty(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
