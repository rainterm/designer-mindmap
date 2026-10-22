package com.designer.admin.module.agent.workflow.activity;

import com.designer.admin.module.agent.util.AgentJson;
import org.noear.solon.annotation.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.applyVars;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.parseCfg;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.str;
/**
 * 输出活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowOutputActivity {

    /** 输出节点：按模板组装最终结果 */
    public String runOutput(String config, Map<String, String> vars) {
        Map<String, Object> cfg = parseCfg(config);
        String type = str(cfg.get("type"), "text").toLowerCase(Locale.ROOT);
        String template = str(cfg.get("template"), "");
        if (template.isBlank() && config != null && !config.trim().startsWith("{")) {
            template = config.trim();
        }
        if (template.isBlank()) {
            template = "{output}";
        }
        String rendered = applyVars(template, vars);
        if ("table".equals(type)) {
            return toMarkdownTable(rendered);
        }
        return rendered;
    }

    /**
     * 将 JSON 数组字符串渲染为 Markdown 表格；非数组或解析失败时原样返回。
     */
    private String toMarkdownTable(String maybeJson) {
        if (maybeJson == null) {
            return "";
        }
        String t = maybeJson.trim();
        if (!t.startsWith("[")) {
            return maybeJson;
        }
        List<Map<String, Object>> rows;
        try {
            rows = AgentJson.parseMapList(t);
        } catch (Exception e) {
            return maybeJson;
        }
        if (rows == null || rows.isEmpty()) {
            return "（表格数据为空）";
        }
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            if (row != null) {
                cols.addAll(row.keySet());
            }
        }
        if (cols.isEmpty()) {
            return "（表格数据为空）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", cols)).append(" |").append('\n');
        StringBuilder sep = new StringBuilder("|");
        for (int i = 0; i < cols.size(); i++) {
            sep.append(" --- |");
        }
        sb.append(sep).append('\n');
        for (Map<String, Object> row : rows) {
            StringBuilder line = new StringBuilder("|");
            for (String c : cols) {
                Object v = row == null ? null : row.get(c);
                String cell = v == null ? "" : String.valueOf(v).replace("|", "\\|").replace("\n", " ").replace("\r", " ");
                line.append(' ').append(cell).append(" |");
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
