package com.designer.admin.module.agent.workflow;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.util.AgentJsonUtil;
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

    /** 合法参数 key 正则 */
    private static final Pattern KEY_OK = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    /** 系统内置参数，始终存在且必填 */
    public static final String SYSTEM_KEY = "query";
    /** 系统内置 query 参数规格 */
    private static final Spec SYSTEM_QUERY = new Spec(SYSTEM_KEY, "查询内容", true, "");

    /** 工具类，禁止实例化。 */
    private AgentWorkflowStartInputs() {
    }

    /**
     * 开始节点单个输入参数定义。
     *
     * @param key          参数 key
     * @param label        展示名
     * @param required     是否必填
     * @param defaultValue 默认值
     */
    public record Spec(String key, String label, boolean required, String defaultValue) {
    }

    /**
     * 从节点列表中找到开始节点并解析其输入定义。
     *
     * @param nodes Vue Flow 节点列表；为 null 时仅返回系统 query
     * @return 输入规格列表（首位始终为系统 query）
     */
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

    /**
     * 解析开始节点 config 中的 inputs 定义。
     *
     * @param config 开始节点 JSON 配置
     * @return 输入规格列表（首位始终为系统 query）
     */
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
        Map<String, Object> map = AgentJsonUtil.parseMap(t);
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

    /**
     * 按开始节点定义合并 params，校验必填。
     *
     * @param specs  输入规格
     * @param params 调用方传入的原始参数
     * @return key → 字符串值（含默认值）
     * @throws BizException 必填项缺失时抛出
     */
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

    /**
     * 清洗参数 key 为合法标识符。
     *
     * @param key 原始 key
     * @return 合法 key；无法清洗时返回空串
     */
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

    /**
     * 将对象转为去空白字符串。
     *
     * @param o 任意对象
     * @return 非 null 字符串
     */
    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
