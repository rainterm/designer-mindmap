package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.dto.workflow.node.OutputNodeConfigDTO;
import com.designer.admin.module.agent.workflow.activity.WorkflowOutputActivity;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Locale;
import java.util.Map;

/**
 * 输出节点：按模板渲染最终回答并推送到回答流。
 *
 * @author 雨夜终端
 */
@Component
public class OutputWorkflowNodeHandler implements WorkflowNodeHandler {

    /** 输出模板渲染活动实现。 */
    @Inject
    private WorkflowOutputActivity outputActivity;

    /**
     * {@inheritDoc}
     * <p>固定返回 {@link WorkflowNodeType#OUTPUT}。
     */
    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.OUTPUT;
    }

    /**
     * {@inheritDoc}
     * <p>提取输出类型与模板文本供试跑面板展示。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表
     * @return 试跑 step.params 字段
     */
    @Override
    public Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        OutputNodeConfigDTO cfg = WorkflowNodeConfigs.parse(configJson, OutputNodeConfigDTO.class);
        Map<String, Object> fallback = WorkflowNodeConfigs.rawConfigFallback(configJson, cfg);
        if (fallback != null) {
            return fallback;
        }
        normalizeForDescribe(cfg);
        return WorkflowNodeConfigs.toParamMap(cfg);
    }

    /**
     * 渲染输出模板，按块推送回答，并返回最终文本。
     *
     * @param ctx 节点执行上下文
     * @return 渲染后的输出文本
     */
    @Override
    public String execute(WorkflowNodeContext ctx) {
        String out = outputActivity.runOutput(ctx.config(), ctx.vars());
        ctx.emitAnswerChunked(out);
        return out;
    }

    /**
     * 试跑面板用：补齐 type / template 默认值。
     *
     * @param cfg 输出节点配置
     */
    private static void normalizeForDescribe(OutputNodeConfigDTO cfg) {
        if (Utils.isBlank(cfg.getType())) {
            cfg.setType("text");
        } else {
            cfg.setType(cfg.getType().toLowerCase(Locale.ROOT));
        }
        if (Utils.isBlank(cfg.getTemplate())) {
            cfg.setTemplate("{output}");
        }
    }
}
