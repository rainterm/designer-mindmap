package com.designer.admin.module.agent.harness.mcp;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.mcp.McpTransportType;
import com.designer.admin.module.agent.model.entity.mcp.AgentMcp;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import org.noear.snack4.ONode;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.ai.mcp.client.McpClientProvider;
import org.noear.solon.ai.mcp.client.McpClientProviders;
import org.noear.solon.ai.mcp.client.McpServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 客户端连接与工具清单拉取。
 *
 * @author 雨夜终端
 */
public final class AgentMcpClientSupport {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(AgentMcpClientSupport.class);

    /** 工具类，禁止实例化。 */
    private AgentMcpClientSupport() {
    }

    /**
     * 将 MCP 实体转为 Solon {@link McpServerParameters}。
     *
     * @param mcp MCP 配置实体
     * @return 客户端连接参数
     */
    public static McpServerParameters toMcpParams(AgentMcp mcp) {
        McpServerParameters p = new McpServerParameters();
        String transport = normalizeTransport(mcp.getTransport(), mcp.getUrl());
        p.setTransport(transport);
        p.setEnabled(true);
        if (isUrlTransport(transport)) {
            p.setUrl(mcp.getUrl());
        } else {
            p.setCommand(mcp.getCommand());
            if (mcp.getArgs() != null && !mcp.getArgs().isBlank()) {
                p.setArgs(Arrays.stream(mcp.getArgs().trim().split("\\s+"))
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList()));
            }
        }
        Map<String, Object> env = AgentJsonUtil.parseMap(mcp.getEnvJson());
        if (!env.isEmpty()) {
            env.forEach((k, v) -> {
                if (k != null && v != null) {
                    p.addEnvVar(k, String.valueOf(v));
                }
            });
        }
        return p;
    }

    /**
     * 归一化传输类型：兼容 JSON 里常见的 streamable_http / http 等别名。
     * Solon 远程通道仅认 sse / streamable / streamable_stateless。
     *
     * @param raw 原始 transport 字符串；可空
     * @param url 远程 URL；有 URL 时未知类型按远程处理
     * @return 归一化后的 transport code
     */
    static String normalizeTransport(String raw, String url) {
        String t = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (t.isEmpty()) {
            return (url != null && !url.isBlank()) ? McpTransportType.STREAMABLE.getCode() : McpTransportType.STDIO.getCode();
        }
        if (McpTransportType.STDIO.eq(t)) {
            return McpTransportType.STDIO.getCode();
        }
        if (McpTransportType.SSE.eq(t)) {
            return McpTransportType.SSE.getCode();
        }
        if (McpTransportType.STREAMABLE_STATELESS.eq(t)) {
            return McpTransportType.STREAMABLE_STATELESS.getCode();
        }
        // http / streamable / streamable_http / streamablehttp …
        if (McpTransportType.HTTP.eq(t)
                || McpTransportType.STREAMABLE.eq(t)
                || McpTransportType.STREAMABLE_HTTP.eq(t)
                || t.contains("streamable")) {
            return McpTransportType.STREAMABLE.getCode();
        }
        // 有 URL 的未知类型按远程处理，避免 Url is empty
        if (url != null && !url.isBlank()) {
            return McpTransportType.STREAMABLE.getCode();
        }
        return t;
    }

    /**
     * 是否为基于 URL 的远程传输（sse / streamable / streamable_stateless）。
     *
     * @param transport 传输类型
     * @return true 表示远程 URL 通道
     */
    static boolean isUrlTransport(String transport) {
        if (transport == null) {
            return false;
        }
        String t = transport.toLowerCase(Locale.ROOT);
        return McpTransportType.SSE.eq(t) || McpTransportType.STREAMABLE.eq(t) || McpTransportType.STREAMABLE_STATELESS.eq(t);
    }

    /**
     * 连接 MCP Server 并拉取工具清单（name / description / inputSchema）。
     *
     * @param mcp MCP 配置实体
     * @return 工具摘要列表
     * @throws BizException 配置缺失或拉取失败时抛出
     */
    public static List<Map<String, Object>> listTools(AgentMcp mcp) {
        if (mcp == null) {
            throw new BizException("MCP 配置不能为空");
        }
        McpServerParameters params = toMcpParams(mcp);
        String transport = params.getTypeOrTransport();
        if (isUrlTransport(transport)) {
            if (mcp.getUrl() == null || mcp.getUrl().isBlank()) {
                throw new BizException("请先填写 URL");
            }
        } else if (mcp.getCommand() == null || mcp.getCommand().isBlank()) {
            throw new BizException("请先填写 Command");
        }

        McpClientProvider client = null;
        try {
            client = McpClientProviders.fromMcpServer(params);
            Collection<FunctionTool> tools = client.getTools();
            List<Map<String, Object>> out = new ArrayList<>();
            if (tools != null) {
                for (FunctionTool t : tools) {
                    if (t == null || t.name() == null || t.name().isBlank()) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", t.name());
                    if (t.title() != null && !t.title().isBlank()) {
                        row.put("title", t.title());
                    }
                    row.put("description", t.description() == null ? "" : t.description());
                    Object schema = parseSchema(t.inputSchema());
                    if (schema != null) {
                        row.put("inputSchema", schema);
                    }
                    out.add(row);
                }
            }
            return out;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("拉取 MCP 工具失败: " + rootMessage(e), e);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.debug("关闭 MCP 客户端忽略", e);
                }
            }
        }
    }

    /**
     * 解析 inputSchema JSON；失败则原样返回字符串。
     *
     * @param schemaJson schema JSON 文本
     * @return 反序列化对象或原始字符串；空则 null
     */
    private static Object parseSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return null;
        }
        try {
            return ONode.deserialize(schemaJson, Object.class);
        } catch (Exception e) {
            return schemaJson;
        }
    }

    /**
     * 提取异常链最深层可读消息。
     *
     * @param e 异常
     * @return 根因消息或简单类名
     */
    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        String msg = e.getMessage();
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                msg = cur.getMessage();
            }
        }
        return msg == null ? e.getClass().getSimpleName() : msg;
    }
}
