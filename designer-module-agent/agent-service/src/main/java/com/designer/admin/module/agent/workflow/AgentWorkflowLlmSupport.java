package com.designer.admin.module.agent.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.harness.AgentChatConfigFactory;
import com.designer.admin.module.agent.harness.AgentMcpClientSupport;
import com.designer.admin.module.agent.harness.AgentSkillMountSupport;
import com.designer.admin.module.agent.harness.AgentToolMapping;
import com.designer.admin.module.agent.mapper.mcp.AgentMcpMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.mapper.skill.AgentSkillMapper;
import com.designer.admin.module.agent.mapper.tool.AgentToolMapper;
import com.designer.admin.module.agent.model.entity.mcp.AgentMcp;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.model.entity.skill.AgentSkill;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.ai.agent.react.ReActAgent;
import org.noear.solon.ai.agent.react.ReActRequest;
import org.noear.solon.ai.agent.react.ReActResponse;
import org.noear.solon.ai.agent.react.task.ReasonDeltaEvent;
import org.noear.solon.ai.agent.session.InMemoryAgentSession;
import org.noear.solon.ai.chat.ChatConfig;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.ai.chat.event.ChatEvent;
import org.noear.solon.ai.chat.event.ChatEventGroup;
import org.noear.solon.ai.harness.HarnessEngine;
import org.noear.solon.ai.harness.agent.AgentDefinition;
import org.noear.solon.ai.harness.agent.ToolName;
import org.noear.solon.ai.talents.mount.MountDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.module.agent.workflow.support.WorkflowVarSupport;
/**
 * 工作流 LLM 节点：按节点配置解析模型，并挂载 Skill / 工具 / MCP。
 *
 * @author 雨夜终端
 */
@Component
public class AgentWorkflowLlmSupport {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowLlmSupport.class);

    @Db
    private AgentProviderMapper providerMapper;
    @Db
    private AgentSkillMapper skillMapper;
    @Db
    private AgentToolMapper toolMapper;
    @Db
    private AgentMcpMapper mcpMapper;
    @Inject("${designer.agent.workspace:./data/agent-harness}")
    private String workspace;

    public String run(Map<String, Object> cfg, Map<String, String> vars, ChatModel fallbackModel) throws Exception {
        return run(cfg, vars, fallbackModel, null, null, null);
    }

    /**
     * @param onToken     非空时消费正文增量
     * @param onThinking  非空时消费思考增量
     * @param thinkingOut 非空时写入完整思考文本
     */
    public String run(Map<String, Object> cfg, Map<String, String> vars, ChatModel fallbackModel,
                      Consumer<String> onToken, Consumer<String> onThinking, StringBuilder thinkingOut)
            throws Exception {
        try {
            return doRun(cfg, vars, fallbackModel, onToken, onThinking, thinkingOut);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            // call()/stream 声明 throws Throwable；Error（如 OOM）必须向上抛出
            if (e instanceof Error err) {
                throw err;
            }
            throw new Exception(e);
        }
    }

    private String doRun(Map<String, Object> cfg, Map<String, String> vars, ChatModel fallbackModel,
                         Consumer<String> onToken, Consumer<String> onThinking, StringBuilder thinkingOut)
            throws Throwable {
        if (cfg == null) {
            cfg = Map.of();
        }
        List<String> skillCodes = stringList(cfg.get("skillCodes"));
        List<String> toolCodes = stringList(cfg.get("toolCodes"));
        List<String> mcpCodes = stringList(cfg.get("mcpCodes"));

        List<AgentSkill> skills = loadSkillsByCodes(skillCodes);
        List<AgentMcp> mcps = loadMcpsByCodes(mcpCodes);
        List<ToolName> toolNames = new ArrayList<>(AgentToolMapping.mapCodes(toolCodes));
        boolean needHarness = !toolNames.isEmpty() || !mcps.isEmpty();
        if (!mcps.isEmpty()) {
            toolNames.add(ToolName.TOOL_MCP);
        }
        if (needHarness && !skills.isEmpty()) {
            toolNames.add(ToolName.TOOL_SKILL);
        }

        String systemTpl = str(cfg.get("systemPrompt"));
        String userTpl = str(cfg.get("userPrompt"));
        if (userTpl.isBlank()) {
            userTpl = "请根据上下文给出回答。\n\n上下文：\n{output}";
        }
        String systemPrompt = WorkflowVarSupport.applyVars(systemTpl, vars);
        String userPrompt = WorkflowVarSupport.applyVars(userTpl, vars);

        if (!skills.isEmpty()) {
            if (needHarness) {
                systemPrompt = systemPrompt.isBlank()
                        ? "你可使用 skillread 读取已挂载的技能后再回答。"
                        : systemPrompt + "\n\n需要时先用 skillread 读取相关技能，再执行任务。";
            } else {
                systemPrompt = appendSkillBodies(systemPrompt, skills);
            }
        }

        ChatConfig chatConfig = resolveChatConfig(cfg);
        boolean stream = onToken != null || onThinking != null || thinkingOut != null;
        if (needHarness) {
            if (chatConfig == null) {
                throw new BizException("使用工具/MCP 时，请在 LLM 节点选择供应商与模型");
            }
            return runWithHarness(chatConfig, str(cfg.get("modelCode")).trim(), systemPrompt, userPrompt,
                    skills, toolNames, mcps, onToken, onThinking, thinkingOut, stream);
        }

        ChatModel model = chatConfig != null ? chatConfig.toChatModel() : fallbackModel;
        if (model == null) {
            throw new BizException("LLM 节点未配置可用模型，请在节点中选择供应商与模型后保存");
        }
        var req = model.prompt(userPrompt);
        if (!systemPrompt.isBlank()) {
            req.systemPrompt(systemPrompt);
        }
        if (!stream) {
            ChatResponse resp = req.call();
            if (resp != null && thinkingOut != null) {
                String th = resp.getThinking();
                if (th != null && !th.isBlank()) {
                    thinkingOut.append(th);
                }
            }
            String text = resp == null ? "" : (resp.getContent() != null ? resp.getContent() : resp.getText());
            return text == null ? "" : text;
        }
        return collectChatStream(req.stream(), onToken, onThinking, thinkingOut);
    }

    private static String collectChatStream(Flux<ChatEvent> flux, Consumer<String> onToken,
                                            Consumer<String> onThinking, StringBuilder thinkingOut) {
        StringBuilder acc = new StringBuilder();
        flux.toStream().forEach(ev -> {
            if (ev == null || !ev.hasText() || !ev.isDelta()) {
                return;
            }
            String t = ev.getText();
            if (t == null || t.isEmpty()) {
                return;
            }
            if (ev.isGroup(ChatEventGroup.THINKING)) {
                if (thinkingOut != null) {
                    thinkingOut.append(t);
                }
                if (onThinking != null) {
                    onThinking.accept(t);
                }
                return;
            }
            if (ev.isGroup(ChatEventGroup.TEXT)) {
                acc.append(t);
                if (onToken != null) {
                    onToken.accept(t);
                }
            }
        });
        return acc.toString();
    }

    private String runWithHarness(ChatConfig chatConfig, String modelCode, String systemPrompt, String userPrompt,
                                  List<AgentSkill> skills, List<ToolName> toolNames, List<AgentMcp> mcps,
                                  Consumer<String> onToken, Consumer<String> onThinking,
                                  StringBuilder thinkingOut, boolean stream)
            throws Throwable {
        Path work = Path.of(workspace == null || workspace.isBlank() ? "./data/agent-harness" : workspace)
                .toAbsolutePath().normalize();
        Files.createDirectories(work);

        HarnessEngine.Builder builder = HarnessEngine.of(work.toString(), ".harness")
                .systemPrompt(systemPrompt == null || systemPrompt.isBlank() ? "你是一个有用的助手。" : systemPrompt)
                .sessionProvider(InMemoryAgentSession::of)
                .hitlEnabled(false)
                .sandboxEnabled(false)
                .memoryEnabled(false)
                .maxTurns(12)
                .modelAdd(chatConfig)
                .defaultModel(modelCode);

        if (!skills.isEmpty()) {
            String bindKey = "wf-" + System.currentTimeMillis();
            MountDir skillMount = AgentSkillMountSupport.materializeBoundSkills(work, bindKey, skills);
            if (skillMount != null) {
                builder.mountAdd(skillMount);
            }
        }
        if (!toolNames.isEmpty()) {
            builder.toolsAdd(toolNames.toArray(ToolName[]::new));
        }
        for (AgentMcp mcp : mcps) {
            try {
                builder.mcpServerAdd(mcp.getCode(), AgentMcpClientSupport.toMcpParams(mcp));
            } catch (Exception e) {
                log.warn("工作流注册 MCP 失败 code={}", mcp.getCode(), e);
            }
        }

        HarnessEngine engine = builder.build();
        AgentDefinition definition = new AgentDefinition();
        definition.setSystemPrompt(systemPrompt);
        definition.getMetadata().setName("workflow-llm");
        definition.getMetadata().setModel(modelCode);
        if (!toolNames.isEmpty()) {
            definition.getMetadata().addTools(toolNames.toArray(ToolName[]::new));
        }
        ReActAgent react = engine.createSubagent(definition).build();
        InMemoryAgentSession session = InMemoryAgentSession.of("wf-" + System.currentTimeMillis());
        ReActRequest request = react.prompt(userPrompt).session(session);
        if (!stream) {
            ReActResponse response = request.call();
            String content = response == null ? null : response.getContent();
            return content == null || content.isBlank() ? "（模型未返回文本内容）" : content;
        }
        StringBuilder acc = new StringBuilder();
        request.stream().toStream().forEach(ev -> {
            if (!(ev instanceof ReasonDeltaEvent delta) || !delta.hasText()) {
                return;
            }
            String t = delta.getText();
            if (t == null || t.isEmpty()) {
                return;
            }
            if (delta.isThinking()) {
                if (thinkingOut != null) {
                    thinkingOut.append(t);
                }
                if (onThinking != null) {
                    onThinking.accept(t);
                }
                return;
            }
            acc.append(t);
            if (onToken != null) {
                onToken.accept(t);
            }
        });
        return acc.isEmpty() ? "（模型未返回文本内容）" : acc.toString();
    }

    private ChatConfig resolveChatConfig(Map<String, Object> cfg) {
        Long providerId = longVal(cfg.get("providerId"));
        String modelCode = str(cfg.get("modelCode")).trim();
        if (providerId == null || modelCode.isBlank()) {
            return null;
        }
        AgentProvider provider = providerMapper.selectById(providerId);
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            throw new BizException("LLM 节点供应商不可用");
        }
        ChatConfig chatConfig = AgentChatConfigFactory.fromProvider(
                provider.getType(), provider.getBaseUrl(), provider.getApiKey(), modelCode);
        Double temperature = doubleVal(cfg.get("temperature"));
        if (temperature != null) {
            chatConfig.addDefaultOption("temperature", temperature);
        }
        Double topP = doubleVal(cfg.get("topP"));
        if (topP != null) {
            chatConfig.addDefaultOption("top_p", topP);
        }
        Integer maxTokens = intVal(cfg.get("maxTokens"));
        if (maxTokens != null && maxTokens > 0) {
            chatConfig.addDefaultOption("max_tokens", maxTokens);
        }
        return chatConfig;
    }

    private List<AgentSkill> loadSkillsByCodes(List<String> codes) {
        if (codes.isEmpty()) {
            return List.of();
        }
        List<AgentSkill> list = skillMapper.selectList(new LambdaQueryWrapper<AgentSkill>()
                .in(AgentSkill::getCode, codes)
                .eq(AgentSkill::getStatus, SystemConsts.STATUS_ENABLE));
        return list == null ? List.of() : list;
    }

    private List<AgentMcp> loadMcpsByCodes(List<String> codes) {
        if (codes.isEmpty()) {
            return List.of();
        }
        List<AgentMcp> list = mcpMapper.selectList(new LambdaQueryWrapper<AgentMcp>()
                .in(AgentMcp::getCode, codes)
                .eq(AgentMcp::getStatus, SystemConsts.STATUS_ENABLE));
        return list == null ? List.of() : list;
    }

    private static String appendSkillBodies(String systemPrompt, List<AgentSkill> skills) {
        StringBuilder sb = new StringBuilder(systemPrompt == null ? "" : systemPrompt.trim());
        for (AgentSkill skill : skills) {
            if (skill == null) {
                continue;
            }
            String body = skill.getContent() == null ? "" : skill.getContent().trim();
            if (body.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("## Skill「").append(skill.getName() == null ? skill.getCode() : skill.getName()).append("」\n");
            sb.append(body);
        }
        return sb.toString();
    }

    private static List<String> stringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        if (raw instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null) {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
        } else if (raw instanceof String s) {
            for (String part : s.split("[,\\s]+")) {
                if (part != null && !part.isBlank()) {
                    out.add(part.trim());
                }
            }
        }
        return new ArrayList<>(out);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Long longVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            String s = String.valueOf(o).trim();
            return s.isEmpty() ? null : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            String s = String.valueOf(o).trim();
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double doubleVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            String s = String.valueOf(o).trim();
            return s.isEmpty() ? null : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
