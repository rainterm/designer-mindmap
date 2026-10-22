package com.designer.admin.module.agent.workflow;

import com.designer.admin.module.agent.util.AgentJson;
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

    private AgentWorkflowCondition() {
    }

    public record Case(String id, String label, String match) {
    }

    public record Spec(List<Case> cases, String elseId) {
    }

    public static Spec parse(String config) {
        String raw = config == null ? "" : config.trim();
        if (raw.startsWith("{")) {
            Map<String, Object> map = AgentJson.parseMap(raw);
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

    public static String pickBranch(Spec spec, Predicate<String> jsTrue) {
        for (Case c : spec.cases()) {
            String expr = c.match() == null ? "" : c.match().trim();
            if (expr.isEmpty() || jsTrue.test(expr)) {
                return c.id();
            }
        }
        return spec.elseId();
    }

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

    private static String sanitizeId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        if (id.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return id;
        }
        return "";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
