package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.dto.workflow.node.KnowledgeNodeConfigDTO;
import com.designer.admin.module.agent.workflow.activity.WorkflowKnowledgeActivity;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Map;

/**
 * 知识库节点：按查询检索知识库并返回拼接后的片段文本。
 *
 * @author 雨夜终端
 */
@Component
public class KnowledgeWorkflowNodeHandler implements WorkflowNodeHandler {

    /** 知识库检索活动实现。 */
    @Inject
    private WorkflowKnowledgeActivity knowledgeActivity;

    /**
     * {@inheritDoc}
     * <p>固定返回 {@link WorkflowNodeType#KNOWLEDGE}。
     */
    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.KNOWLEDGE;
    }

    /**
     * {@inheritDoc}
     * <p>提取 knowledgeId、query、topK、scoreThreshold 等试跑参数。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表
     * @return 试跑 step.params 字段
     */
    @Override
    public Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        KnowledgeNodeConfigDTO cfg = WorkflowNodeConfigs.parse(configJson, KnowledgeNodeConfigDTO.class);
        Map<String, Object> fallback = WorkflowNodeConfigs.rawConfigFallback(configJson, cfg);
        if (fallback != null) {
            return fallback;
        }
        return WorkflowNodeConfigs.toParamMap(cfg);
    }

    /**
     * 检索知识库并返回结果文本。
     *
     * @param ctx 节点执行上下文
     * @return 检索结果拼接文本
     */
    @Override
    public String execute(WorkflowNodeContext ctx) {
        return knowledgeActivity.runKnowledge(ctx.config(), ctx.vars());
    }
}
