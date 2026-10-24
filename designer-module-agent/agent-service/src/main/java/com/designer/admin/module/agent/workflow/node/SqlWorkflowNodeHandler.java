package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.dto.workflow.node.SqlNodeConfigDTO;
import com.designer.admin.module.agent.workflow.activity.WorkflowSqlActivity;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Map;

/**
 * SQL 节点：执行配置的 SQL 并返回查询/更新结果文本。
 *
 * @author 雨夜终端
 */
@Component
public class SqlWorkflowNodeHandler implements WorkflowNodeHandler {

    /** SQL 执行活动实现。 */
    @Inject
    private WorkflowSqlActivity sqlActivity;

    /**
     * {@inheritDoc}
     * <p>固定返回 {@link WorkflowNodeType#SQL}。
     */
    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.SQL;
    }

    /**
     * {@inheritDoc}
     * <p>提取待执行的 SQL 文本供试跑面板展示。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表
     * @return 试跑 step.params 字段
     */
    @Override
    public Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        SqlNodeConfigDTO cfg = WorkflowNodeConfigs.parse(configJson, SqlNodeConfigDTO.class);
        Map<String, Object> fallback = WorkflowNodeConfigs.rawConfigFallback(configJson, cfg);
        if (fallback != null) {
            return fallback;
        }
        return WorkflowNodeConfigs.toParamMap(cfg);
    }

    /**
     * 执行 SQL 并返回结果文本。
     *
     * @param ctx 节点执行上下文
     * @return SQL 执行结果文本
     * @throws Exception SQL 执行失败时抛出
     */
    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        return sqlActivity.runSql(ctx.config(), ctx.vars());
    }
}
