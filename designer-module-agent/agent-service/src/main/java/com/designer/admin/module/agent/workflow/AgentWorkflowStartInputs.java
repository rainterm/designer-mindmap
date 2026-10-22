package com.designer.admin.module.agent.workflow;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.util.AgentJson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 解析开始节点输入参数定义，并合并试跑入参。
 *
 * @author 雨夜终端
 */
public final class AgentWorkflowStartInputs {

    private static final Pattern KEY_OK = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    /** 系统内置参数，始终存在且必填 */
    public static final String SYSTEM_KEY = "query";
    private static final Spec SYSTEM_QUERY = new Spec(SYSTEM_KEY, "查询内容", true, "");

    private AgentWorkflowStartInputs() {
    }

    public record Spec(String key, String label, boolean required, String defaultValue) {
    }

    public static List<Spec> parseFromNodes(List<Map<String, Object>> nodes) {
        if (nodes == null) {
            return List.of(SYSTEM_QUERY);
        }
        for (Map<String, Object> n : nodes) {
            if (WorkflowNodeType.START.eqIgnoreCase(str(n.get("type")))) {
                return parseConfig(str(n.get("config")));
            }
        }
        return List.of(SYSTEM_QUERY);
    }

    public static List<Spec> parseConfig(String config) {
        List<Spec> specs = new ArrayList<>();
        specs.add(SYSTEM_QUERY);
        if (config == null || config.isBlank()) {
            return specs;
        }
        String t = config.trim();
        if (!t.startsWith("{")) {
            return specs;
        }
        Map<String, Object> map = AgentJson.parseMap(t);
        Object raw = map.get("inputs");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return specs;
        }
        int i = 0;
        for (Object item : list) {
            i++;
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String key = sanitizeKey(str(m.get("key")));
            if (key.isBlank()) {
                key = "arg" + i;
            }
            // 系统参数以固定定义为准，忽略配置中的重复项
            if (SYSTEM_KEY.equals(key)) {
                continue;
            }
            String label = str(m.get("label"));
            if (label.isBlank()) {
                label = key;
            }
            boolean required = Boolean.parseBoolean(String.valueOf(m.get("required")));
            String def = str(m.get("defaultValue"));
            specs.add(new Spec(key, label, required, def));
        }
        return specs;
    }

    /** 按开始节点定义合并 params，校验必填。 */
    public static Map<String, String> resolve(List<Spec> specs, Map<String, Object> params) {
        List<Spec> list = specs == null ? List.of() : specs;
        Map<String, Object> src = params == null ? Map.of() : params;
        Map<String, String> out = new LinkedHashMap<>();
        for (Spec spec : list) {
            Object raw = src.get(spec.key());
            String val;
            if (raw != null && !String.valueOf(raw).isBlank()) {
                val = String.valueOf(raw);
            } else if (spec.defaultValue() != null && !spec.defaultValue().isBlank()) {
                val = spec.defaultValue();
            } else {
                val = raw == null ? "" : String.valueOf(raw);
            }
            if (spec.required() && (val == null || val.isBlank())) {
                throw new BizException("缺少必填输入参数: " + spec.label() + " (" + spec.key() + ")");
            }
            out.put(spec.key(), val == null ? "" : val);
        }
        return out;
    }

    private static String sanitizeKey(String key) {
        if (key == null) {
            return "";
        }
        String k = key.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        if (k.isEmpty()) {
            return "";
        }
        if (!KEY_OK.matcher(k).matches()) {
            k = "p_" + k;
        }
        return k;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
