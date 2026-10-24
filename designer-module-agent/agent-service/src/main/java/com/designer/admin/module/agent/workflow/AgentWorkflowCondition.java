package com.designer.admin.module.agent.workflow;

import com.designer.admin.module.agent.util.AgentJsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 条件节点：如果 / 否则如果… / 否则。兼容旧版单关键字 + true/false 出口。
 *
 * @author 雨夜终端
 */
public final class AgentWorkflowCondition {

    /** 工具类，禁止实例化。 */
    private AgentWorkflowCondition() {
    }

    /**
     * 单个条件分支。
     *
     * @param id    分支 id（链路 when 匹配用）
     * @param label 展示标签
     * @param match JS 条件表达式；空则视为命中
     */
    public record Case(String id, String label, String match) {
    }

    /**
     * 条件节点规格。
     *
     * @param cases  有序分支列表（如果 / 否则如果）
     * @param elseId 否则分支 id
     */
    public record Spec(List<Case> cases, String elseId) {
    }

    /**
     * 解析条件节点配置。
     *
     * @param config JSON（含 cases）或旧版单表达式字符串
     * @return 解析后的规格；无法解析多分支时退化为 true/false
     */
    public static Spec parse(String config) {
        String raw = config == null ? "" : config.trim();
        if (raw.startsWith("{")) {
            Map<String, Object> map = AgentJsonUtil.parseMap(raw);
            Object list = map.get("cases");
            if (list instanceof List<?> items && !items.isEmpty()) {
                List<Case> cases = new ArrayList<>();
                int i = 0;
                for (Object item : items) {
                    i++;
                    if (!(item instanceof Map<?, ?> m)) {
                        continue;
                    }
                    String id = sanitizeId(str(m.get("id")));
                    if (id.isBlank()) {
                        id = "elif" + i;
                    }
                    if ("else".equals(id) || "false".equals(id)) {
                        continue;
                    }
                    cases.add(new Case(id, str(m.get("label")), str(m.get("match"))));
                }
                if (!cases.isEmpty()) {
                    return new Spec(cases, "else");
                }
            }
        }
        return new Spec(List.of(new Case("true", "", raw)), "false");
    }

    /**
     * 按顺序求值分支表达式，返回命中的分支 id；均未命中则返回否则分支。
     *
     * @param spec   条件规格
     * @param jsTrue 表达式求值器，返回 true 表示命中
     * @return 分支 id
     */
    public static String pickBranch(Spec spec, Predicate<String> jsTrue) {
        for (Case c : spec.cases()) {
            String expr = c.match() == null ? "" : c.match().trim();
            if (expr.isEmpty() || jsTrue.test(expr)) {
                return c.id();
            }
        }
        return spec.elseId();
    }

    /**
     * 将边的 sourceHandle / 标签解析为分支 id。
     *
     * @param handle 边手柄或标签；可为 yes/no/是/否 等别名
     * @param spec   条件规格
     * @param index  出边序号（handle 为空时按序映射到 cases）
     * @return 分支 id
     */
    public static String resolveLinkBranch(String handle, Spec spec, int index) {
        String h = handle == null ? "" : handle.trim();
        if (!h.isBlank()) {
            String l = h.toLowerCase(Locale.ROOT);
            if ("yes".equals(l) || "y".equals(l) || "是".equals(l) || "通过".equals(l)) {
                return spec.cases().isEmpty() ? "true" : spec.cases().get(0).id();
            }
            if ("no".equals(l) || "n".equals(l) || "否".equals(l) || "拒绝".equals(l)
                    || "else".equals(l) || "否则".equals(l)) {
                return spec.elseId();
            }
            return h;
        }
        if (index >= 0 && index < spec.cases().size()) {
            return spec.cases().get(index).id();
        }
        return spec.elseId();
    }

    /**
     * 校验并清洗分支 id（仅允许标识符字符）。
     *
     * @param id 原始 id
     * @return 合法 id；非法时返回空串
     */
    private static String sanitizeId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        if (id.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return id;
        }
        return "";
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
