package com.designer.admin.module.agent.harness.tool;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.core.AppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合 {@link FunctionToolProvider}；新增 Provider 只需声明为 Bean。
 *
 * <p>使用 {@link AppContext#subBeansOfType} 收集（与 FileStorageProviderRegistry 一致），
 * 避免 {@code @Inject List<>} 在 Solon 下拿不到实现类。</p>
 *
 * @author 雨夜终端
 */
@Component
public class FunctionToolProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(FunctionToolProviderRegistry.class);

    @Inject
    private AppContext appContext;

    /** 已收集的领域工具提供者列表（不可变快照） */
    private List<FunctionToolProvider> providers = List.of();

    @Init
    public void init() {
        List<FunctionToolProvider> list = new ArrayList<>();
        appContext.subBeansOfType(FunctionToolProvider.class, list::add);
        this.providers = List.copyOf(list);
        if (providers.isEmpty()) {
            log.warn("未发现任何 FunctionToolProvider 实现");
        } else {
            log.info("已注册 {} 个 FunctionToolProvider", providers.size());
        }
    }

    /**
     * 聚合所有支持当前上下文的 Provider，构建 FunctionTool 列表。
     *
     * @param ctx 构建上下文
     * @return 工具列表；无可用项时为空列表
     */
    public List<FunctionTool> buildAll(FunctionToolBuildContext ctx) {
        if (ctx == null || providers.isEmpty()) {
            return List.of();
        }
        List<FunctionTool> out = new ArrayList<>();
        for (FunctionToolProvider p : providers) {
            if (p == null || !p.supports(ctx)) {
                continue;
            }
            List<FunctionTool> tools = p.build(ctx);
            if (tools != null && !tools.isEmpty()) {
                out.addAll(tools);
            }
        }
        return out;
    }

    /**
     * 聚合各 Provider 的 system prompt 附录。
     *
     * @param ctx 构建上下文
     * @return 拼接后的附录文案；无内容时为空串
     */
    public String systemPromptAppendix(FunctionToolBuildContext ctx) {
        if (ctx == null || providers.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (FunctionToolProvider p : providers) {
            if (p == null || !p.supports(ctx)) {
                continue;
            }
            String appendix = p.systemPromptAppendix(ctx);
            if (appendix != null && !appendix.isBlank()) {
                sb.append(appendix);
            }
        }
        return sb.toString();
    }
}
