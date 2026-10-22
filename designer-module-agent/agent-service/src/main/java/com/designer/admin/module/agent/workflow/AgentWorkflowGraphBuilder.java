package com.designer.admin.module.agent.workflow;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.util.AgentJson;
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

    private AgentWorkflowGraphBuilder() {
    }

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
                String config = AgentJson.configAsString(n.get("config"));
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
                    AgentWorkflowCondition.Spec spec = AgentWorkflowCondition.parse(AgentJson.configAsString(n.get("config")));
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

    private static String escape(String s) {
        return s.replace("'", "");
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }
}
