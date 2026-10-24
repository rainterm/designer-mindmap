package com.designer.admin.module.agent.harness.tool;

import org.noear.solon.ai.chat.tool.FunctionTool;

import java.util.List;

/**
 * 领域自定义 FunctionTool 提供者。新增领域工具时实现本接口并注册为 {@code @Component}。
 *
 * @author 雨夜终端
 */
public interface FunctionToolProvider {

    /**
     * 当前上下文是否应贡献工具。
     *
     * @param ctx 构建上下文
     * @return true 表示应调用 {@link #build}
     */
    boolean supports(FunctionToolBuildContext ctx);

    /**
     * 按上下文构建本领域的 FunctionTool 列表。
     *
     * @param ctx 构建上下文
     * @return 工具列表；无工具时可返回空列表
     */
    List<FunctionTool> build(FunctionToolBuildContext ctx);

    /**
     * 有工具激活时追加到 system prompt；默认空。
     *
     * @param ctx 构建上下文
     * @return 追加文案；无内容时返回空串
     */
    default String systemPromptAppendix(FunctionToolBuildContext ctx) {
        return "";
    }
}
