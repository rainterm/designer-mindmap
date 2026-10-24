package com.designer.admin.module.agent.workflow;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.NodeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 将管理端 Vue Flow 图（nodes/edges）转换为 Solon Flow {@link Graph}。
 *
 * @author 雨夜终端
 */
public final class AgentWorkflowGraphBuilder {

    /** 工具类，禁止实例化。 */
    private AgentWorkflowGraphBuilder() {
    }

    /**
     * 构建 Solon Flow 图。
     *
     * @param workflowId 工作流 id（用于 graphId；可为 null）
     * @param title      图标题；空白时用 graphId
     * @param nodes      Vue Flow 节点列表
     * @param edges      Vue Flow 边列表
     * @param tasks      活动 / 条件节点任务工厂
     * @return Solon Flow Graph
     * @throws BizException 无节点或缺少开始节点时抛出
     */
    @SuppressWarnings("unchecked")
    public static Graph build(Long workflowId, String title,
                              List<Map<String, Object>> nodes,
                              List<Map<String, Object>> edges,
                              AgentWorkflowNodeTasks tasks) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BizException("工作流没有节点");
        }
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        String startKey = null;
        for (Map<String, Object> n : nodes) {
            String key = str(n.get("key"));
            if (key.isBlank()) {
                continue;
            }
            nodeMap.put(key, n);
            if (WorkflowNodeType.START.eqIgnoreCase(str(n.get("type"))) && startKey == null) {
                startKey = key;
            }
        }
        if (startKey == null) {
            throw new BizException("工作流缺少开始节点");
        }

        Map<String, List<Map<String, Object>>> outEdges = new HashMap<>();
        if (edges != null) {
            for (Map<String, Object> e : edges) {
                String source = str(e.get("source"));
                String target = str(e.get("target"));
                if (source.isBlank() || target.isBlank()) {
                    continue;
                }
                if (!nodeMap.containsKey(source) || !nodeMap.containsKey(target)) {
                    continue;
                }
                outEdges.computeIfAbsent(source, k -> new ArrayList<>()).add(e);
            }
        }

        String graphId = "agent-wf-" + (workflowId == null ? "tmp" : workflowId);
        String graphTitle = title == null || title.isBlank() ? graphId : title;

        return Graph.create(graphId, graphTitle, g -> {
            Set<String> added = new HashSet<>();
            for (Map.Entry<String, Map<String, Object>> entry : nodeMap.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> n = entry.getValue();
                String type = str(n.get("type")).toLowerCase(Locale.ROOT);
                String name = str(n.get("name"));
                if (name.isBlank()) {
                    name = key;
                }
                String config = AgentJsonUtil.configAsString(n.get("config"));
                WorkflowNodeType nodeType = WorkflowNodeType.fromCode(type);
                String outputVar = WorkflowNodeType.OUTPUT == nodeType ? "" : str(n.get("outputVar"));
                NodeSpec spec;
                if (nodeType == WorkflowNodeType.START) {
                    spec = g.addStart(key).title(name);
                } else if (nodeType == WorkflowNodeType.END) {
                    spec = g.addEnd(key).title(name);
                } else if (nodeType == WorkflowNodeType.CONDITION) {
                    spec = g.addExclusive(key).title(name)
                            .task(tasks.conditionTask(key, name, config));
                } else {
                    spec = g.addActivity(key).title(name)
                            .task(tasks.activityTask(key, type, name, config, outputVar));
                }
                added.add(key);
                // meta 便于调试
                spec.metaPut("nodeType", type);
                spec.metaPut("config", config);
                spec.metaPut("outputVar", outputVar);
            }

            for (String key : added) {
                Map<String, Object> n = nodeMap.get(key);
                String type = str(n.get("type")).toLowerCase(Locale.ROOT);
                List<Map<String, Object>> outs = outEdges.getOrDefault(key, List.of());
                NodeSpec from = g.getNode(key);
                if (from == null) {
                    continue;
                }
                if (WorkflowNodeType.CONDITION.eq(type)) {
                    AgentWorkflowCondition.Spec spec = AgentWorkflowCondition.parse(AgentJsonUtil.configAsString(n.get("config")));
                    int i = 0;
                    for (Map<String, Object> e : outs) {
                        String target = str(e.get("target"));
                        String handle = firstNonBlank(str(e.get("sourceHandle")), str(e.get("label")));
                        final int idx = i++;
                        String branch = AgentWorkflowCondition.resolveLinkBranch(handle, spec, idx);
                        from.linkAdd(target, link -> {
                            link.when("branch == '" + escape(branch) + "'");
                            link.title(handle.isBlank() ? branch : handle);
                        });
                    }
                } else {
                    for (Map<String, Object> e : outs) {
                        from.linkAdd(str(e.get("target")));
                    }
                }
            }
        });
    }

    /**
     * 去掉单引号，避免 when 表达式注入。
     *
     * @param s 原始字符串
     * @return 清洗后字符串
     */
    private static String escape(String s) {
        return s.replace("'", "");
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

    /**
     * 返回第一个非空白字符串。
     *
     * @param a 优先值
     * @param b 回退值
     * @return 非 null 字符串
     */
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }
}
