package com.designer.admin.module.agent.workflow.node;

import com.designer.admin.module.agent.constant.workflow.WorkflowNodeType;
import com.designer.admin.module.agent.model.dto.workflow.node.HttpNodeConfigDTO;
import com.designer.admin.module.agent.workflow.activity.WorkflowHttpActivity;
import com.designer.admin.module.agent.workflow.support.WorkflowVarSupport;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP 节点：发起外部 HTTP 请求并返回响应文本。
 *
 * @author 雨夜终端
 */
@Component
public class HttpWorkflowNodeHandler implements WorkflowNodeHandler {

    /** HTTP 请求活动实现。 */
    @Inject
    private WorkflowHttpActivity httpActivity;

    /**
     * {@inheritDoc}
     * <p>固定返回 {@link WorkflowNodeType#HTTP}。
     */
    @Override
    public WorkflowNodeType type() {
        return WorkflowNodeType.HTTP;
    }

    /**
     * {@inheritDoc}
     * <p>描述 method/url/headers/body 等试跑参数，并对 URL 与表单做变量替换。
     *
     * @param configJson 节点配置 JSON
     * @param vars       当前工作流变量表
     * @return 试跑 step.params 字段
     */
    @Override
    public Map<String, Object> describeParams(String configJson, Map<String, String> vars) {
        HttpNodeConfigDTO cfg = WorkflowNodeConfigs.parse(configJson, HttpNodeConfigDTO.class);
        Map<String, Object> fallback = WorkflowNodeConfigs.rawConfigFallback(configJson, cfg);
        if (fallback != null) {
            return fallback;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        String method = Utils.isBlank(cfg.getMethod()) ? "GET" : cfg.getMethod().toUpperCase(Locale.ROOT);
        String url = WorkflowVarSupport.applyVars(cfg.getUrl() == null ? "" : cfg.getUrl(), vars);
        boolean bodyMethod = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
        if (!bodyMethod) {
            url = WorkflowHttpActivity.appendQueryParams(
                    url, WorkflowHttpActivity.resolveFormFields(cfg, vars));
        }
        params.put("method", method.isBlank() ? "GET" : method);
        params.put("url", url);
        String bodyType = cfg.getBodyType();
        if (Utils.isBlank(bodyType)) {
            bodyType = bodyMethod ? "json" : "form";
        }
        params.put("bodyType", bodyType);
        params.put("timeoutSec", cfg.getTimeoutSec());
        params.put("headers", WorkflowHttpActivity.resolveHeaders(cfg, vars));
        if (cfg.getFormFields() != null) {
            params.put("formFields", cfg.getFormFields());
        } else if (bodyMethod) {
            params.put("body", cfg.getBody() == null ? "" : cfg.getBody());
        }
        return params;
    }

    /**
     * 执行 HTTP 请求并返回响应体文本。
     *
     * @param ctx 节点执行上下文
     * @return HTTP 响应文本
     * @throws Exception 请求失败时抛出
     */
    @Override
    public String execute(WorkflowNodeContext ctx) throws Exception {
        return httpActivity.runHttp(ctx.config(), ctx.vars());
    }
}
