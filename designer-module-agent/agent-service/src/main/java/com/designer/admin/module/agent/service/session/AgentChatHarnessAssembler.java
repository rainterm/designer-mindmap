package com.designer.admin.module.agent.service.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.constant.session.AgentMessageRole;
import com.designer.admin.module.agent.harness.AgentChatConfigFactory;
import com.designer.admin.module.agent.harness.AgentMcpClientSupport;
import com.designer.admin.module.agent.harness.AgentSkillMountSupport;
import com.designer.admin.module.agent.harness.AgentToolMapping;
import com.designer.admin.module.agent.harness.WorkflowFunctionTool;
import com.designer.admin.module.agent.mapper.mcp.AgentMcpMapper;
import com.designer.admin.module.agent.mapper.session.AgentMessageMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.mapper.skill.AgentSkillMapper;
import com.designer.admin.module.agent.mapper.tool.AgentToolMapper;
import com.designer.admin.module.agent.model.dto.session.ChatToolCallDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.mcp.AgentMcp;
import com.designer.admin.module.agent.model.entity.session.AgentMessage;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.model.entity.skill.AgentSkill;
import com.designer.admin.module.agent.model.entity.tool.AgentTool;
import com.designer.admin.module.agent.model.entity.workflow.AgentWorkflow;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeRetrieveService;
import com.designer.admin.module.agent.service.knowledge.RagRetrieveOptions;
import com.designer.admin.module.agent.service.knowledge.RagRetrieveResult;
import com.designer.admin.module.agent.util.AgentJson;
import com.designer.admin.module.agent.workflow.AgentWorkflowExecutor;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.agent.session.InMemoryAgentSession;
import org.noear.solon.ai.chat.ChatConfig;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.ai.harness.HarnessEngine;
import org.noear.solon.ai.harness.agent.ToolName;
import org.noear.solon.ai.talents.mount.MountDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 对话 Harness 组装：供应商 / 工具 / MCP / Skill / RAG / 工作流工具 / 历史记忆。
 *
 * @author 雨夜终端
 */
@Component
public class AgentChatHarnessAssembler {

    private static final Logger log = LoggerFactory.getLogger(AgentChatHarnessAssembler.class);
    private static final int KNOWLEDGE_BUDGET = 8000;

    @Inject("${designer.agent.workspace:./data/agent-harness}")
    private String workspace;

    @Db
    private AgentProviderMapper providerMapper;
    @Db
    private AgentToolMapper toolMapper;
    @Db
    private AgentMcpMapper mcpMapper;
    @Db
    private AgentSkillMapper skillMapper;
    @Db
    private AgentMessageMapper messageMapper;
    @Inject
    private AgentWorkflowExecutor workflowExecutor;
    @Inject
    private AgentKnowledgeRetrieveService knowledgeRetrieveService;

    /**
     * 组装一次对话所需的 Harness 上下文（不含 ReAct 流式执行）。
     */
    public Prepared prepare(AgentAgent agent, String userContent,
                            Consumer<String> onStatus, Consumer<ChatToolCallDTO> onTool) {
        if (agent == null) {
            throw new BizException("智能体不存在");
        }
        if (agent.getProviderId() == null) {
            throw new BizException("请先为智能体配置模型供应商");
        }
        AgentProvider provider = providerMapper.selectById(agent.getProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            throw new BizException("模型供应商不可用");
        }
        String modelCode = agent.getModelCode();
        if (modelCode == null || modelCode.isBlank()) {
            throw new BizException("请先为智能体选择模型");
        }
        ChatConfig chatConfig;
        try {
            chatConfig = AgentChatConfigFactory.fromProvider(
                    provider.getType(), provider.getBaseUrl(), provider.getApiKey(), modelCode);
        } catch (IllegalArgumentException e) {
            throw new BizException(e.getMessage(), e);
        }

        List<String> toolCodes = loadToolCodes(AgentJson.parseLongList(agent.getToolIdsJson()));
        List<ToolName> toolNames = new ArrayList<>(AgentToolMapping.mapCodes(toolCodes));
        List<AgentMcp> mcps = loadMcps(AgentJson.parseLongList(agent.getMcpIdsJson()));
        if (!mcps.isEmpty()) {
            toolNames.add(ToolName.TOOL_MCP);
        }
        List<AgentSkill> skills = loadSkills(AgentJson.parseLongList(agent.getSkillIdsJson()));
        if (!skills.isEmpty()) {
            toolNames.add(ToolName.TOOL_SKILL);
        }
        applyAgentModelOptions(chatConfig, agent);

        List<ChatToolCallDTO> ragTools = new ArrayList<>();
        String knowledgeBlock = buildKnowledgeBlock(agent, userContent, onStatus, onTool, ragTools);
        String systemPrompt = buildSystemPrompt(agent.getSystemPrompt(), knowledgeBlock, !skills.isEmpty());

        Path work = Path.of(workspace == null || workspace.isBlank() ? "./data/agent-harness" : workspace)
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(work);
        } catch (Exception e) {
            throw new BizException("无法创建 Harness 工作区: " + e.getMessage(), e);
        }

        emitStatus(onStatus, "正在准备对话…");

        HarnessEngine.Builder builder = HarnessEngine.of(work.toString(), ".harness")
                .systemPrompt(systemPrompt)
                .sessionProvider(InMemoryAgentSession::of)
                .hitlEnabled(false)
                .sandboxEnabled(false)
                .memoryEnabled(false)
                .maxTurns(12)
                .modelAdd(chatConfig)
                .defaultModel(modelCode.trim());

        if (!skills.isEmpty()) {
            try {
                MountDir skillMount = AgentSkillMountSupport.materializeBoundSkills(work, agent.getId(), skills);
                if (skillMount != null) {
                    builder.mountAdd(skillMount);
                }
            } catch (Exception e) {
                log.error("物化 Skill 失败 agentId={}", agent.getId(), e);
                throw new BizException("Skill 加载失败: " + e.getMessage(), e);
            }
        }
        if (!toolNames.isEmpty()) {
            builder.toolsAdd(toolNames.toArray(ToolName[]::new));
        }
        for (AgentMcp mcp : mcps) {
            try {
                builder.mcpServerAdd(mcp.getCode(), AgentMcpClientSupport.toMcpParams(mcp));
            } catch (Exception e) {
                log.warn("注册 MCP 失败 code={}", mcp.getCode(), e);
            }
        }

        emitStatus(onStatus, skills.isEmpty() ? "正在调用模型…" : "正在加载 Skill 并调用模型…");

        List<FunctionTool> workflowTools = buildWorkflowTools(agent, chatConfig, onStatus);
        Map<String, String> toolLabels = loadToolLabels(toolCodes);
        toolLabels.putIfAbsent("skillread", "读取 Skill");
        toolLabels.putIfAbsent("skilllist", "列出 Skill");
        toolLabels.putIfAbsent("skillsearch", "搜索 Skill");
        toolLabels.putIfAbsent("skillrefresh", "刷新 Skill");
        toolLabels.putIfAbsent("knowledge_retrieve", "知识库检索");
        for (FunctionTool t : workflowTools) {
            toolLabels.putIfAbsent(t.name(), t.title());
        }

        return new Prepared(
                chatConfig,
                modelCode.trim(),
                systemPrompt,
                toolNames,
                ragTools,
                toolLabels,
                workflowTools,
                builder.build());
    }

    public void hydrateHistory(InMemoryAgentSession harnessSession, Long sessionId,
                               String currentUserContent, Integer memoryRounds) {
        int rounds = normalizeMemoryRounds(memoryRounds);
        if (rounds <= 0) {
            return;
        }
        int fetchLimit = Math.min(AgentConsts.MAX_MEMORY_ROUNDS * 2 + 4, rounds * 2 + 4);
        List<AgentMessage> history = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByDesc(AgentMessage::getId)
                .last("LIMIT " + fetchLimit));
        if (history == null || history.isEmpty()) {
            return;
        }
        Collections.reverse(history);
        AgentMessage last = history.get(history.size() - 1);
        if (AgentMessageRole.USER.eqIgnoreCase(last.getRole())
                && Objects.equals(last.getContent(), currentUserContent == null ? "" : currentUserContent)) {
            history = history.subList(0, history.size() - 1);
        }
        history = trimToMemoryRounds(history, rounds);
        List<ChatMessage> msgs = new ArrayList<>();
        for (AgentMessage m : history) {
            if (m.getContent() == null) {
                continue;
            }
            if (AgentMessageRole.USER.eqIgnoreCase(m.getRole())) {
                msgs.add(ChatMessage.ofUser(m.getContent()));
            } else if (AgentMessageRole.ASSISTANT.eqIgnoreCase(m.getRole())) {
                msgs.add(ChatMessage.ofAssistant(m.getContent()));
            } else if (AgentMessageRole.SYSTEM.eqIgnoreCase(m.getRole())) {
                msgs.add(ChatMessage.ofSystem(m.getContent()));
            }
        }
        if (!msgs.isEmpty()) {
            harnessSession.addMessage(msgs);
        }
    }

    public record Prepared(
            ChatConfig chatConfig,
            String modelCode,
            String systemPrompt,
            List<ToolName> toolNames,
            List<ChatToolCallDTO> ragTools,
            Map<String, String> toolLabels,
            List<FunctionTool> workflowTools,
            HarnessEngine engine
    ) {
    }

    private static void applyAgentModelOptions(ChatConfig chatConfig, AgentAgent agent) {
        if (chatConfig == null || agent == null) {
            return;
        }
        if (agent.getTemperature() != null) {
            chatConfig.addDefaultOption("temperature", agent.getTemperature().doubleValue());
        }
        if (agent.getTopP() != null) {
            chatConfig.addDefaultOption("top_p", agent.getTopP().doubleValue());
        }
        Integer maxTokens = agent.getMaxTokens();
        if (maxTokens != null && maxTokens > 0) {
            chatConfig.addDefaultOption("max_tokens", maxTokens);
        }
    }

    private static String buildSystemPrompt(String base, String knowledgeBlock, boolean hasSkills) {
        StringBuilder sb = new StringBuilder();
        if (base != null && !base.isBlank()) {
            sb.append(base.trim());
        } else {
            sb.append("你是一个有用的智能助手。");
        }
        if (hasSkills) {
            sb.append("\n\n## 输出要求（必须遵守）\n")
                    .append("- 先用 skillread 读取相关技能，再执行任务。\n")
                    .append("- 思考过程保持简短；不要把设计讨论、英文草稿写进最终回复正文。\n")
                    .append("- 最终回复正文只保留：一句中文说明 + 一个完整的 ```html ... ``` 代码块。\n")
                    .append("- 代码块必须是可直接打开的完整 HTML 文档（含 <!DOCTYPE html> 与 </html>，CSS/JS 内联）。\n")
                    .append("- 禁止输出未完成的半截代码；若篇幅不够，优先保证 HTML 完整，再写说明。\n");
        }
        if (knowledgeBlock != null && !knowledgeBlock.isBlank()) {
            sb.append("\n\n## Knowledge\n以下为绑定知识库摘录，回答时优先参考：\n")
                    .append(knowledgeBlock);
        }
        return sb.toString();
    }

    private String buildKnowledgeBlock(AgentAgent agent, String userQuery,
                                       Consumer<String> onStatus, Consumer<ChatToolCallDTO> onTool,
                                       List<ChatToolCallDTO> toolsOut) {
        List<Long> knowledgeIds = AgentJson.parseLongList(agent.getKnowledgeIdsJson());
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return "";
        }
        String query = userQuery == null ? "" : userQuery.trim();
        String callId = "kb-" + System.currentTimeMillis();
        ChatToolCallDTO start = new ChatToolCallDTO();
        start.setCallId(callId);
        start.setPhase("start");
        start.setToolName("knowledge_retrieve");
        start.setToolLabel("知识库检索");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("query", query);
        args.put("knowledgeIds", knowledgeIds);
        start.setArgs(args);
        long t0 = System.currentTimeMillis();
        emitStatus(onStatus, "正在检索知识库…");
        emitTool(onTool, start);
        if (toolsOut != null) {
            toolsOut.add(start);
        }
        try {
            RagRetrieveResult result = knowledgeRetrieveService.retrieveDetailed(
                    knowledgeIds, query, KNOWLEDGE_BUDGET, RagRetrieveOptions.fromAgent(agent));
            ChatToolCallDTO end = new ChatToolCallDTO();
            end.setCallId(callId);
            end.setPhase("end");
            end.setToolName("knowledge_retrieve");
            end.setToolLabel("知识库检索");
            Map<String, Object> endArgs = new LinkedHashMap<>(args);
            endArgs.put("hitCount", result.hitCount());
            List<Map<String, Object>> sources = buildKnowledgeSources(result);
            if (!sources.isEmpty()) {
                endArgs.put("sources", sources);
            }
            end.setArgs(endArgs);
            end.setDurationMs(System.currentTimeMillis() - t0);
            if (result.hit()) {
                end.setResult("命中 " + result.hitCount() + " 条分块\n\n" + result.block());
                emitStatus(onStatus, "知识库命中 " + result.hitCount() + " 条");
            } else {
                end.setResult("未命中相关分块（可检查绑定知识库、索引状态、TopK/相似度阈值）");
                emitStatus(onStatus, "知识库未命中");
            }
            emitTool(onTool, end);
            if (toolsOut != null) {
                upsertTool(toolsOut, end);
            }
            return result.block() == null ? "" : result.block();
        } catch (Exception e) {
            log.warn("知识库向量检索失败", e);
            ChatToolCallDTO end = new ChatToolCallDTO();
            end.setCallId(callId);
            end.setPhase("end");
            end.setToolName("knowledge_retrieve");
            end.setToolLabel("知识库检索");
            end.setArgs(args);
            end.setDurationMs(System.currentTimeMillis() - t0);
            end.setError(e.getMessage() == null ? "检索失败" : e.getMessage());
            emitTool(onTool, end);
            if (toolsOut != null) {
                upsertTool(toolsOut, end);
            }
            emitStatus(onStatus, "知识库检索失败");
            return "";
        }
    }

    private List<FunctionTool> buildWorkflowTools(AgentAgent agent, ChatConfig chatConfig,
                                                  Consumer<String> onStatus) {
        List<Long> ids = AgentJson.parseLongList(agent.getWorkflowIdsJson());
        if (ids.isEmpty()) {
            return List.of();
        }
        List<AgentWorkflow> list = workflowExecutor.listBoundWorkflows(ids);
        if (list.isEmpty()) {
            return List.of();
        }
        ChatModel chatModel = chatConfig.toChatModel();
        List<FunctionTool> tools = new ArrayList<>();
        for (AgentWorkflow wf : list) {
            tools.add(new WorkflowFunctionTool(wf, workflowExecutor, chatModel, onStatus));
        }
        return tools;
    }

    private Map<String, String> loadToolLabels(List<String> toolCodes) {
        Map<String, String> map = new HashMap<>();
        if (toolCodes == null || toolCodes.isEmpty()) {
            return map;
        }
        List<AgentTool> tools = toolMapper.selectList(new LambdaQueryWrapper<AgentTool>()
                .in(AgentTool::getCode, toolCodes));
        if (tools == null) {
            return map;
        }
        for (AgentTool t : tools) {
            if (t != null && t.getCode() != null) {
                map.put(t.getCode(), t.getName() == null || t.getName().isBlank() ? t.getCode() : t.getName());
            }
        }
        return map;
    }

    private List<String> loadToolCodes(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        List<AgentTool> tools = toolMapper.selectByIds(toolIds);
        if (tools == null) {
            return List.of();
        }
        return tools.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == SystemConsts.STATUS_ENABLE)
                .map(AgentTool::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<AgentMcp> loadMcps(List<Long> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return List.of();
        }
        List<AgentMcp> list = mcpMapper.selectByIds(mcpIds);
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == SystemConsts.STATUS_ENABLE)
                .collect(Collectors.toList());
    }

    private List<AgentSkill> loadSkills(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return List.of();
        }
        List<AgentSkill> list = skillMapper.selectByIds(skillIds);
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == SystemConsts.STATUS_ENABLE)
                .collect(Collectors.toList());
    }

    private static List<AgentMessage> trimToMemoryRounds(List<AgentMessage> history, int rounds) {
        if (history == null || history.isEmpty() || rounds <= 0) {
            return List.of();
        }
        int userCount = 0;
        int start = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (AgentMessageRole.USER.eqIgnoreCase(history.get(i).getRole())) {
                userCount++;
                if (userCount >= rounds) {
                    start = i;
                    break;
                }
            }
        }
        return start <= 0 ? history : new ArrayList<>(history.subList(start, history.size()));
    }

    private static int normalizeMemoryRounds(Integer v) {
        if (v == null) {
            return AgentConsts.DEFAULT_MEMORY_ROUNDS;
        }
        if (v < 0) {
            return 0;
        }
        return Math.min(AgentConsts.MAX_MEMORY_ROUNDS, v);
    }

    /** 按文档去重，供前端在回答底部展示可点击来源。 */
    private static List<Map<String, Object>> buildKnowledgeSources(RagRetrieveResult result) {
        if (result == null || result.hits() == null || result.hits().isEmpty()) {
            return List.of();
        }
        Map<Long, Map<String, Object>> byDoc = new LinkedHashMap<>();
        for (RagRetrieveResult.Hit h : result.hits()) {
            if (h == null || h.docId() <= 0 || byDoc.containsKey(h.docId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("knowledgeId", h.knowledgeId());
            row.put("docId", h.docId());
            row.put("title", h.docTitle() == null || h.docTitle().isBlank() ? ("文档 #" + h.docId()) : h.docTitle());
            row.put("kbName", h.kbName() == null ? "" : h.kbName());
            row.put("score", h.score());
            byDoc.put(h.docId(), row);
        }
        return new ArrayList<>(byDoc.values());
    }

    private static void upsertTool(List<ChatToolCallDTO> tools, ChatToolCallDTO dto) {
        if (tools == null || dto == null) {
            return;
        }
        if (dto.getCallId() != null) {
            for (int i = 0; i < tools.size(); i++) {
                ChatToolCallDTO old = tools.get(i);
                if (dto.getCallId().equals(old.getCallId())) {
                    tools.set(i, dto);
                    return;
                }
            }
        }
        tools.add(dto);
    }

    private static void emitStatus(Consumer<String> onStatus, String text) {
        if (onStatus != null && text != null && !text.isBlank()) {
            onStatus.accept(text);
        }
    }

    private static void emitTool(Consumer<ChatToolCallDTO> onTool, ChatToolCallDTO dto) {
        if (onTool != null && dto != null) {
            onTool.accept(dto);
        }
    }
}
