package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.dto.workflow.node.CodeNodeConfigDTO;
import com.designer.admin.module.agent.workflow.support.WorkflowCodeEngineSupport;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Map;

/**
 * 代码节点：在沙箱中执行脚本并返回输出文本。
 *
 * @author 雨夜终端
 */
@Component
public class CodeWorkflowNodeHandler implements WorkflowNodeHandler {

    /** 代码引擎执行支持。 */
    @Inject
    private WorkflowCodeEngineSupport codeEngineSupport;

    /**
     * {@inheritDoc}
     * <p>固定返回 {@link WorkflowNodeType#CODE}。
     */
    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.CODE;
    }

    /**
     * {@inheritDoc}
     * <p>提取 language、script 等试跑参数。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表
     * @return 试跑 step.params 字段
     */
    @Override
    public Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        CodeNodeConfigDTO cfg = WorkflowNodeConfigs.parse(configJson, CodeNodeConfigDTO.class);
        Map<String, Object> fallback = WorkflowNodeConfigs.rawConfigFallback(configJson, cfg);
        if (fallback != null) {
            return fallback;
        }
        normalizeForDescribe(cfg);
        return WorkflowNodeConfigs.toParamMap(cfg);
    }

    /**
     * 执行脚本并返回输出文本。
     *
     * @param ctx 节点执行上下文
     * @return 脚本执行结果文本
     * @throws Exception 脚本执行失败时抛出
     */
    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        return codeEngineSupport.runCode(ctx.config(), ctx.vars());
    }

    /**
     * 试跑面板用：补齐 language 默认值。
     *
     * @param cfg 代码节点配置
     */
    private static void normalizeForDescribe(CodeNodeConfigDTO cfg) {
        if (Utils.isBlank(cfg.getLanguage())) {
            cfg.setLanguage("js");
        }
        if (cfg.getScript() == null) {
            cfg.setScript("");
        }
    }
}
