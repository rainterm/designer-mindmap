package com.designer.admin.module.agent.workflow.support;

import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流变量替换、路径取值与配置解析。
 *
 * @author 雨夜终端
 */
public final class WorkflowVarSupport {

    /** 变量路径：root 或 root.path / root[0] */
    private static final Pattern VAR_PATH = Pattern.compile(
            "^([a-zA-Z_][a-zA-Z0-9_]*)((?:\\.[a-zA-Z_][a-zA-Z0-9_]*|\\[\\d+\\])*)$");
    /** JSON 路径段：.field 或 [index] */
    private static final Pattern JSON_PATH_SEG = Pattern.compile("\\.([a-zA-Z_][a-zA-Z0-9_]*)|\\[(\\d+)]");
    /** 路径取值：{output.access_token}、{obj.field}、{var.items[0].id} */
    private static final Pattern JSON_PATH_PLACEHOLDER = Pattern.compile(
            "\\{([a-zA-Z_][a-zA-Z0-9_]*)((?:\\.[a-zA-Z_][a-zA-Z0-9_]*|\\[\\d+\\])+)\\}");
    /**
     * SQL 参数占位：'{a.b}' / "{a}" / {a.b[0]} / {key}，包裹引号一并替换为 ?
     */
    private static final Pattern SQL_PLACEHOLDER = Pattern.compile(
            "'\\{([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*|\\[\\d+\\])*)\\}'"
                    + "|\"\\{([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*|\\[\\d+\\])*)\\}\""
                    + "|\\{([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*|\\[\\d+\\])*)\\}");
    /** 标识符替换 ${name}，仅允许安全标识符字符 */
    private static final Pattern SQL_IDENTIFIER = Pattern.compile(
            "\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    /** SQL 标识符白名单正则 */
    private static final Pattern SAFE_IDENT = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /** 工具类，禁止实例化。 */
    private WorkflowVarSupport() {
    }

    /**
     * 将 SQL 模板中的变量转为预编译参数：{@code {var}} / {@code '{var}'} → {@code ?}；
     * {@code ${ident}} 用于表名等标识符（白名单校验后直接替换）。
     *
     * @param template SQL 模板
     * @param vars     变量表
     * @return 绑定后的 SQL 与参数列表
     * @throws IllegalArgumentException 标识符变量非法时抛出
     */
    public static BoundSql bindSql(String template, Map<String, String> vars) {
        if (template == null) {
            return new BoundSql("", List.of());
        }
        List<Object> params = new ArrayList<>();
        Matcher m = SQL_PLACEHOLDER.matcher(template);
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
            String expr = m.group(1) != null ? m.group(1) : (m.group(2) != null ? m.group(2) : m.group(3));
            params.add(evalPath(expr, vars));
            m.appendReplacement(buf, "?");
        }
        m.appendTail(buf);
        String sql = buf.toString();
        Matcher idm = SQL_IDENTIFIER.matcher(sql);
        StringBuffer idBuf = new StringBuffer();
        while (idm.find()) {
            String name = idm.group(1);
            String val = vars == null ? null : vars.get(name);
            if (val == null || !SAFE_IDENT.matcher(val).matches()) {
                throw new IllegalArgumentException("非法 SQL 标识符变量: " + name);
            }
            idm.appendReplacement(idBuf, Matcher.quoteReplacement(val));
        }
        idm.appendTail(idBuf);
        return new BoundSql(idBuf.toString(), params);
    }

    /**
     * SQL 预编译绑定结果。
     *
     * @param sql    绑定后的 SQL（占位符为 {@code ?}）
     * @param params 与占位符顺序对应的参数
     * @author 雨夜终端
     */
    public record BoundSql(String sql, List<Object> params) {
    }

    /**
     * 替换模板中的路径占位符与简单 {@code {key}} 变量。
     *
     * @param template 模板文本
     * @param vars     变量表
     * @return 替换后文本；template 为 null 时返回空串
     */
    public static String applyVars(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        String result = template;
        Matcher pathMatcher = JSON_PATH_PLACEHOLDER.matcher(result);
        StringBuffer pathBuf = new StringBuffer();
        while (pathMatcher.find()) {
            String root = pathMatcher.group(1);
            String path = pathMatcher.group(2);
            String json = lookupJsonRoot(vars, root);
            String val = readJsonPath(json, path);
            pathMatcher.appendReplacement(pathBuf, Matcher.quoteReplacement(val == null ? "" : val));
        }
        pathMatcher.appendTail(pathBuf);
        result = pathBuf.toString();
        if (vars != null) {
            List<String> keys = new ArrayList<>(vars.keySet());
            keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
            for (String key : keys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                String val = vars.get(key);
                result = result.replace("{" + key + "}", val == null ? "" : val);
            }
        }
        return result;
    }

    /**
     * 解析条件/取值表达式：result.code、obj.field、{output.items[0].id}，以及字面量。
     *
     * @param expr 表达式
     * @param vars 变量表
     * @return 求值结果字符串
     */
    public static String evalPath(String expr, Map<String, String> vars) {
        if (expr == null) {
            return "";
        }
        String s = expr.trim();
        if (s.startsWith("{") && s.endsWith("}") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.isEmpty()) {
            return "";
        }
        if (vars != null && vars.containsKey(s)) {
            String v = vars.get(s);
            return v == null ? "" : v;
        }
        Matcher m = VAR_PATH.matcher(s);
        if (!m.matches()) {
            return s;
        }
        String root = m.group(1);
        String path = m.group(2);
        if (path == null || path.isBlank()) {
            if (vars != null && vars.containsKey(root)) {
                String v = vars.get(root);
                return v == null ? "" : v;
            }
            if ("obj".equals(root) && vars != null) {
                String v = vars.get(AgentConsts.CTX_OUTPUT);
                return v == null ? "" : v;
            }
            return s;
        }
        return readJsonPath(lookupJsonRoot(vars, root), path);
    }

    /**
     * 按根名查找 JSON 字符串；{@code obj} 回退到 output。
     *
     * @param vars 变量表
     * @param root 根变量名
     * @return JSON 文本；未找到时 null
     */
    public static String lookupJsonRoot(Map<String, String> vars, String root) {
        if (vars != null && vars.containsKey(root)) {
            return vars.get(root);
        }
        if ("obj".equals(root) && vars != null) {
            return vars.get(AgentConsts.CTX_OUTPUT);
        }
        return null;
    }

    /**
     * 判断字符串是否像 JSON 对象或数组。
     *
     * @param s 文本
     * @return 是则 true
     */
    public static boolean isJsonValue(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    /**
     * 沿路径读取 JSON 字段或数组元素，结果转为字符串。
     *
     * @param json JSON 文本
     * @param path 如 {@code .field} 或 {@code [0].id}
     * @return 取值字符串；失败时空串
     */
    public static String readJsonPath(String json, String path) {
        if (json == null || json.isBlank() || path == null || path.isBlank() || !isJsonValue(json)) {
            return "";
        }
        String t = json.trim();
        Object cur;
        try {
            cur = t.startsWith("[") ? AgentJsonUtil.parseArray(t) : AgentJsonUtil.parseMap(t);
        } catch (Exception e) {
            return "";
        }
        List<String> segs = splitJsonPath(path);
        for (String seg : segs) {
            if (cur == null) {
                return "";
            }
            if (cur instanceof Map<?, ?> map) {
                cur = map.get(seg);
            } else if (cur instanceof List<?> list) {
                int idx;
                try {
                    idx = Integer.parseInt(seg);
                } catch (NumberFormatException e) {
                    return "";
                }
                if (idx < 0 || idx >= list.size()) {
                    return "";
                }
                cur = list.get(idx);
            } else {
                return "";
            }
        }
        if (cur == null) {
            return "";
        }
        if (cur instanceof String s) {
            return s;
        }
        if (cur instanceof Number || cur instanceof Boolean) {
            return String.valueOf(cur);
        }
        return AgentJsonUtil.toJson(cur);
    }

    /**
     * 将路径拆为字段名 / 下标段列表。
     *
     * @param path 如 {@code .a[0].b}
     * @return 段列表
     */
    public static List<String> splitJsonPath(String path) {
        List<String> segs = new ArrayList<>();
        Matcher m = JSON_PATH_SEG.matcher(path);
        while (m.find()) {
            segs.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return segs;
    }

    /**
     * 从上下文组装变量表（开始节点参数 + output）。
     *
     * @param ctx       Flow 上下文
     * @param outputKey 流水线输出键
     * @param varsKey   入参 Map 键
     * @return 变量表（含 output）
     */
    public static Map<String, String> varsOf(org.noear.solon.flow.FlowContext ctx,
                                             String outputKey, String varsKey) {
        Map<String, String> vars = new LinkedHashMap<>();
        Object raw = ctx.get(varsKey);
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null) {
                    vars.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
                }
            });
        }
        vars.put(AgentConsts.CTX_OUTPUT, ctx.get(outputKey) == null ? "" : String.valueOf(ctx.get(outputKey)));
        return vars;
    }

    /**
     * 转为字符串；null 或空白时返回默认值。
     *
     * @param o   任意对象
     * @param def 默认值
     * @return 字符串
     */
    public static String str(Object o, String def) {
        if (o == null) {
            return def;
        }
        String s = String.valueOf(o);
        return s.isBlank() ? def : s;
    }

    /**
     * 超长文本缩略，末尾加省略号。
     *
     * @param s   原文
     * @param max 最大保留长度
     * @return 缩略后文本
     */
    public static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
