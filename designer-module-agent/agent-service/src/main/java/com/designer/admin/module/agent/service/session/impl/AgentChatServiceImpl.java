package com.designer.admin.module.agent.service.session.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.noear.solon.ai.agent.AgentEvent;
import org.noear.solon.ai.agent.react.ReActAgent;
import org.noear.solon.ai.agent.react.ReActRequest;
import org.noear.solon.ai.agent.react.ReActResponse;
import org.noear.solon.ai.agent.react.RunEndEvent;
import org.noear.solon.ai.agent.react.task.ReasonDeltaEvent;
import org.noear.solon.ai.agent.react.task.ToolCallEndEvent;
import org.noear.solon.ai.agent.react.task.ToolCallStartEvent;
import org.noear.solon.ai.agent.session.InMemoryAgentSession;
import org.noear.solon.ai.chat.content.ContentBlock;
import org.noear.solon.ai.chat.content.ImageBlock;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.prompt.Prompt;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.ai.harness.HarnessEngine;
import org.noear.solon.ai.harness.agent.AgentDefinition;
import org.noear.solon.ai.harness.agent.ToolName;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.harness.context.MmProjectChatContext;
import com.designer.admin.module.agent.harness.tool.MmProjectFunctionTool;
import com.designer.admin.module.agent.model.dto.session.ChatAttachmentDTO;
import com.designer.admin.module.agent.model.dto.session.ChatStreamResult;
import com.designer.admin.module.agent.model.dto.session.ChatToolCallDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.session.AgentSession;
import com.designer.admin.module.agent.service.session.AgentChatHarnessAssembler;
import com.designer.admin.module.agent.service.session.AgentChatService;
import com.designer.admin.module.agent.service.session.AgentChatAnswerSupport;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import com.designer.admin.module.mindmap.api.MmProjectApi;
import com.designer.admin.module.mindmap.api.model.MmProjectBriefVO;
import reactor.core.publisher.Flux;

/**
 * 基于 Solon AI Harness 的调试对话执行。
 *
 * @author 雨夜终端
 */
@Component
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatServiceImpl.class);
    private static final int TOOL_RESULT_LIMIT = 6000;
    private static final int THINKING_LIMIT = 100_000;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\"'\\]\\)\\]]+");
    private static final Pattern MD_HEADING = Pattern.compile("(?m)^#{1,6}\\s+\\S+");

    @Inject
    private AgentChatHarnessAssembler harnessAssembler;
    @Inject
    private MmProjectApi mmProjectApi;

    @Override
    public String chat(AgentSession session, AgentAgent agent, String userContent) {
        return chatStream(session, agent, userContent, List.of(), null, null, null, null).getContent();
    }

    @Override
    public String chat(AgentSession session, AgentAgent agent, String userContent,
                       List<ChatAttachmentDTO> attachments) {
        return chatStream(session, agent, userContent, attachments, null, null, null, null).getContent();
    }

    @Override
    public ChatStreamResult chatStream(AgentSession session, AgentAgent agent, String userContent,
                                       List<ChatAttachmentDTO> attachments, Consumer<String> onDelta,
                                       Consumer<ChatToolCallDTO> onTool) {
        return chatStream(session, agent, userContent, attachments, onDelta, onTool, null, null);
    }

    @Override
    public ChatStreamResult chatStream(AgentSession session, AgentAgent agent, String userContent,
                                       List<ChatAttachmentDTO> attachments, Consumer<String> onDelta,
                                       Consumer<ChatToolCallDTO> onTool, Consumer<String> onStatus) {
        return chatStream(session, agent, userContent, attachments, onDelta, onTool, onStatus, null);
    }

    @Override
    public ChatStreamResult chatStream(AgentSession session, AgentAgent agent, String userContent,
                                       List<ChatAttachmentDTO> attachments, Consumer<String> onDelta,
                                       Consumer<ChatToolCallDTO> onTool, Consumer<String> onStatus,
                                       Consumer<String> onThinking) {
        ChatStreamResult out = new ChatStreamResult();
        if (agent == null) {
            throw new BizException("智能体不存在");
        }

        AgentChatHarnessAssembler.Prepared prepared = harnessAssembler.prepare(agent, userContent, onStatus, onTool);
        HarnessEngine engine = prepared.engine();
        String systemPrompt = prepared.systemPrompt();
        String modelCode = prepared.modelCode();
        List<ToolName> toolNames = prepared.toolNames();
        List<ChatToolCallDTO> ragTools = prepared.ragTools();
        Map<String, String> toolLabels = prepared.toolLabels();
            List<FunctionTool> extraTools = prepared.extraTools() == null
                    ? List.of()
                    : prepared.extraTools();

        try {
            AgentDefinition definition = new AgentDefinition();
            definition.setSystemPrompt(systemPrompt);
            definition.getMetadata().setName("agent-" + agent.getId());
            definition.getMetadata().setModel(modelCode);
            if (!toolNames.isEmpty()) {
                definition.getMetadata().addTools(toolNames.toArray(ToolName[]::new));
            }

            ReActAgent.Builder agentBuilder = engine.createSubagent(definition);
            if (!extraTools.isEmpty()) {
                agentBuilder.defaultToolAdd(extraTools);
                log.debug("已挂载自定义工具 sessionId={} tools={}",
                        session.getId(),
                        extraTools.stream().map(FunctionTool::name).toList());
            }
            ReActAgent react = agentBuilder.build();
            InMemoryAgentSession harnessSession = InMemoryAgentSession.of("s-" + session.getId());
            // send 已先落库当前 user 消息，hydrate 时剔除末条以免与 prompt 重复
            harnessAssembler.hydrateHistory(harnessSession, session.getId(), userContent, agent.getMemoryRounds());

            String promptText = composeUserPrompt(userContent, attachments);
            promptText = appendMindmapBoundHint(promptText);
            ReActRequest request = buildPromptRequest(react, promptText, attachments)
                    .session(harnessSession);
            if (onDelta == null && onTool == null && onStatus == null && onThinking == null) {
                ReActResponse response = request.call();
                String content = response == null ? null : response.getContent();
                if (content == null || content.isBlank()) {
                    out.setContent("（模型未返回文本内容）");
                } else {
                    out.setContent(content);
                }
                List<ChatToolCallDTO> tools = new ArrayList<>(ragTools);
                applyMindmapSaveFallback(out, tools, onTool, onStatus);
                out.setTools(tools);
                return out;
            }

            StringBuilder acc = new StringBuilder();
            StringBuilder thinkingAcc = new StringBuilder();
            List<ChatToolCallDTO> tools = new ArrayList<>(ragTools);
            // [0]=thinking 已提示，[1]=generating 已提示（阶段切换时重置）
            boolean[] phaseFlags = {false, false};
            Flux<AgentEvent> flux = request.stream();
            flux.doOnNext(event -> handleStreamEvent(
                            event, acc, thinkingAcc, tools, toolLabels,
                            onDelta, onTool, onStatus, onThinking, phaseFlags))
                    .blockLast(Duration.ofMinutes(10));
            String cleaned = AgentChatAnswerSupport.sanitizeAnswerContent(acc.toString(), thinkingAcc.toString());
            if (cleaned == null || cleaned.isBlank()) {
                out.setContent("（模型未返回文本内容）");
            } else {
                if (!cleaned.equals(acc.toString().trim())) {
                    log.info("已清洗混入正文的思考内容 sessionId={} agentId={}", session.getId(), agent.getId());
                }
                out.setContent(cleaned);
            }
            out.setThinking(thinkingAcc.toString());
            applyMindmapSaveFallback(out, tools, onTool, onStatus);
            out.setTools(tools);
            return out;
        } catch (BizException e) {
            throw e;
        } catch (Throwable e) {
            log.error("Harness 对话失败 sessionId={} agentId={}", session.getId(), agent.getId(), e);
            throw new BizException("对话调用失败: " + rootMessage(e));
        }
    }

    private static ReActRequest buildPromptRequest(ReActAgent react, String promptText,
                                                   List<ChatAttachmentDTO> attachments) {
        List<ContentBlock> images = imageBlocks(attachments);
        if (images.isEmpty()) {
            return react.prompt(promptText);
        }
        return react.prompt(Prompt.of(ChatMessage.ofUser(promptText, images)));
    }

    private static List<ContentBlock> imageBlocks(List<ChatAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<ContentBlock> blocks = new ArrayList<>();
        for (ChatAttachmentDTO a : attachments) {
            if (a == null || !isImageAttachment(a)) {
                continue;
            }
            String mime = a.getContentType() == null || a.getContentType().isBlank()
                    ? "image/jpeg"
                    : a.getContentType();
            if (a.getBase64() != null && !a.getBase64().isBlank()) {
                blocks.add(ImageBlock.ofBase64(a.getBase64().trim(), mime));
            } else if (a.getUrl() != null && !a.getUrl().isBlank()) {
                blocks.add(ImageBlock.ofUrl(a.getUrl().trim(), mime));
            }
        }
        return blocks;
    }

    private static boolean isImageAttachment(ChatAttachmentDTO a) {
        String ct = a.getContentType() == null ? "" : a.getContentType().toLowerCase(Locale.ROOT);
        if (ct.startsWith("image/")) {
            return true;
        }
        String name = a.getName() == null ? "" : a.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp");
    }

    private static String composeUserPrompt(String userContent, List<ChatAttachmentDTO> attachments) {
        String text = userContent == null ? "" : userContent;
        if (attachments == null || attachments.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        if (!text.isBlank()) {
            sb.append(text.trim());
        }
        sb.append("\n\n## 附件\n");
        for (ChatAttachmentDTO a : attachments) {
            if (a == null) {
                continue;
            }
            String name = a.getName() == null || a.getName().isBlank() ? "未命名文件" : a.getName();
            sb.append("\n### ").append(name).append("\n");
            if (a.getUrl() != null && !a.getUrl().isBlank()) {
                sb.append("URL: ").append(a.getUrl().trim()).append("\n");
            }
            if (a.getTextContent() != null && !a.getTextContent().isBlank()) {
                sb.append("```\n").append(a.getTextContent().trim()).append("\n```\n");
            } else if (isImageAttachment(a)) {
                sb.append("（图片附件，已随消息提供给模型）\n");
            } else {
                sb.append("（二进制附件，请结合文件名与 URL 理解）\n");
            }
        }
        return sb.toString().trim();
    }

    /** 编辑页绑定导图时追加硬约束，避免模型只聊大纲、征求确认而不调用落库工具。 */
    private static String appendMindmapBoundHint(String promptText) {
        Long bound = MmProjectChatContext.getProjectId();
        if (bound == null || bound <= 0) {
            return promptText == null ? "" : promptText;
        }
        String base = promptText == null ? "" : promptText.trim();
        String hint = "【系统约束】当前编辑导图 projectId=" + bound
                + "。整理大纲后必须立刻调用 mm_project_save 覆盖更新（无需传 projectId），"
                + "禁止征求确认，禁止只输出大纲而不调用工具。";
        if (base.isEmpty()) {
            return hint;
        }
        return base + "\n\n" + hint;
    }

    /**
     * 编辑页兜底：模型未调 mm_project_save 但正文含 Markdown 标题树时，服务端直接落库并补一条工具事件（触发画布刷新）。
     */
    private void applyMindmapSaveFallback(ChatStreamResult out, List<ChatToolCallDTO> tools,
                                          Consumer<ChatToolCallDTO> onTool, Consumer<String> onStatus) {
        Long bound = MmProjectChatContext.getProjectId();
        if (bound == null || bound <= 0 || mmProjectApi == null || out == null) {
            return;
        }
        if (hasSuccessfulMindmapSave(tools)) {
            return;
        }
        String outline = extractMarkdownOutline(out.getContent());
        if (outline == null) {
            return;
        }
        try {
            emitStatus(onStatus, "正在写入思维导图…");
            String callId = "mm-fallback-" + System.currentTimeMillis();
            ChatToolCallDTO start = new ChatToolCallDTO();
            start.setCallId(callId);
            start.setPhase("start");
            start.setToolName(MmProjectFunctionTool.CODE);
            start.setToolLabel("保存思维导图");
            start.setArgs(Map.of("outline", outline));
            tools.add(start);
            emitTool(onTool, start);

            MmProjectBriefVO brief = mmProjectApi.updateFromOutline(bound, outline, null);
            String resultJson = AgentJsonUtil.toJson(Map.of(
                    "projectId", brief.getId(),
                    "name", brief.getName() == null ? "" : brief.getName(),
                    "editPath", brief.getEditPath() == null ? "" : brief.getEditPath(),
                    "fallback", true
            ));
            ChatToolCallDTO end = new ChatToolCallDTO();
            end.setCallId(callId);
            end.setPhase("end");
            end.setToolName(MmProjectFunctionTool.CODE);
            end.setToolLabel("保存思维导图");
            end.setResult(resultJson);
            upsertTool(tools, end);
            emitTool(onTool, end);
            out.setContent("已根据大纲更新当前思维导图。");
            emitStatus(onStatus, "思维导图已更新");
            log.info("导图编辑页兜底落库成功 projectId={}", bound);
        } catch (Exception e) {
            log.warn("导图编辑页兜底落库失败 projectId={}", bound, e);
        }
    }

    private static boolean hasSuccessfulMindmapSave(List<ChatToolCallDTO> tools) {
        if (tools == null || tools.isEmpty()) {
            return false;
        }
        for (ChatToolCallDTO t : tools) {
            if (t == null || t.getError() != null && !t.getError().isBlank()) {
                continue;
            }
            if (!MmProjectFunctionTool.isMindmapToolCode(t.getToolName())) {
                continue;
            }
            if ("start".equalsIgnoreCase(t.getPhase())) {
                continue;
            }
            String result = t.getResult();
            if (result != null && !result.isBlank() && !result.contains("\"ok\":false")) {
                return true;
            }
        }
        return false;
    }

    /** 正文中提取可落库的 Markdown 标题树；标题不足则返回 null。 */
    private static String extractMarkdownOutline(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher block = Pattern.compile("(?is)```(?:markdown|md)?\\s*\\n([\\s\\S]*?)```").matcher(content);
        while (block.find()) {
            String body = block.group(1) == null ? "" : block.group(1).trim();
            if (countMdHeadings(body) >= 2) {
                return body;
            }
        }
        String trimmed = content.trim();
        if (countMdHeadings(trimmed) >= 2) {
            return trimmed;
        }
        return null;
    }

    private static int countMdHeadings(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int n = 0;
        Matcher m = MD_HEADING.matcher(text);
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static void handleStreamEvent(AgentEvent event, StringBuilder acc, StringBuilder thinkingAcc,
                                          List<ChatToolCallDTO> tools, Map<String, String> toolLabels,
                                          Consumer<String> onDelta, Consumer<ChatToolCallDTO> onTool,
                                          Consumer<String> onStatus, Consumer<String> onThinking,
                                          boolean[] phaseFlags) {
        if (event instanceof ToolCallStartEvent start) {
            ChatToolCallDTO dto = toToolStart(start, toolLabels);
            upsertTool(tools, dto);
            emitTool(onTool, dto);
            resetPhaseFlags(phaseFlags);
            String label = dto.getToolLabel() == null || dto.getToolLabel().isBlank()
                    ? dto.getToolName()
                    : dto.getToolLabel();
            emitStatus(onStatus, "正在调用「" + label + "」…");
            return;
        }
        if (event instanceof ToolCallEndEvent end) {
            ChatToolCallDTO dto = toToolEnd(end, toolLabels);
            upsertTool(tools, dto);
            emitTool(onTool, dto);
            resetPhaseFlags(phaseFlags);
            emitStatus(onStatus, "工具已完成，继续生成…");
            return;
        }
        if (event instanceof ReasonDeltaEvent delta) {
            if (!delta.hasText()) {
                return;
            }
            if (delta.isThinking()) {
                String text = delta.getText();
                appendCapped(thinkingAcc, text, THINKING_LIMIT);
                emitThinking(onThinking, text);
                if (phaseFlags != null && phaseFlags.length > 0 && !phaseFlags[0]) {
                    phaseFlags[0] = true;
                    if (phaseFlags.length > 1) {
                        phaseFlags[1] = false;
                    }
                    emitStatus(onStatus, acc.length() > 0 ? "继续思考中…" : "正在思考…");
                }
                return;
            }
            if (phaseFlags != null && phaseFlags.length > 0) {
                phaseFlags[0] = false;
            }
            String text = delta.getText();
            acc.append(text);
            emitDelta(onDelta, text);
            if (phaseFlags != null && phaseFlags.length > 1 && !phaseFlags[1]) {
                phaseFlags[1] = true;
                emitStatus(onStatus, "正在生成…");
            }
            return;
        }
        if (event instanceof RunEndEvent end) {
            // 纯思考轮结束后，框架可能把 thinking 降级为 finalAnswer；有思考面板时不要并入正文
            mergeFinalAnswer(acc, thinkingAcc, end.getText(), onDelta);
        }
    }

    private static void appendCapped(StringBuilder sb, String text, int limit) {
        if (sb == null || text == null || text.isEmpty() || sb.length() >= limit) {
            return;
        }
        int remain = limit - sb.length();
        if (text.length() <= remain) {
            sb.append(text);
        } else {
            sb.append(text, 0, remain);
        }
    }

    /**
     * 将 RunEnd 终态文本合并进流式累计。
     * 若终态实质是思考降级内容，则不再写入正文（思考过程已单独推送）。
     */
    private static void mergeFinalAnswer(StringBuilder acc, StringBuilder thinkingAcc, String finalText,
                                         Consumer<String> onDelta) {
        if (finalText == null || finalText.isBlank()) {
            return;
        }
        String text = finalText.trim();
        String thinking = thinkingAcc == null ? "" : thinkingAcc.toString().trim();
        if (!thinking.isEmpty() && AgentChatAnswerSupport.isThinkingLikeFinal(text, thinking)) {
            if (acc.length() == 0) {
                String tip = "（模型仅返回了思考过程，未产出完整正文。可清空 Max Tokens 不限制，或调高后重试。）";
                acc.append(tip);
                emitDelta(onDelta, tip);
            }
            return;
        }
        if (AgentChatAnswerSupport.isPlanningDump(text) && !AgentChatAnswerSupport.containsCompleteHtml(text)) {
            if (acc.length() == 0) {
                String tip = "（模型未产出完整 HTML。可清空 Max Tokens 不限制，或调高后重试。）";
                acc.append(tip);
                emitDelta(onDelta, tip);
            }
            return;
        }
        String current = acc.toString();
        if (current.isEmpty()) {
            acc.append(text);
            emitDelta(onDelta, text);
            return;
        }
        if (current.contains(text) || text.equals(current.trim())) {
            return;
        }
        if (text.startsWith(current)) {
            String suffix = text.substring(current.length());
            if (!suffix.isEmpty()) {
                acc.append(suffix);
                emitDelta(onDelta, suffix);
            }
            return;
        }
        // 真实终态与已流式正文不同时再追加
        if (AgentChatAnswerSupport.containsCompleteHtml(text) || text.contains("```")) {
            String add = "\n\n" + text;
            acc.append(add);
            emitDelta(onDelta, add);
        }
    }

    private static ChatToolCallDTO toToolStart(ToolCallStartEvent start, Map<String, String> toolLabels) {
        ChatToolCallDTO dto = new ChatToolCallDTO();
        dto.setCallId(start.getCallId());
        dto.setPhase("start");
        dto.setToolName(start.getToolName());
        dto.setToolLabel(toolLabels.getOrDefault(start.getToolName(), start.getToolName()));
        if (start.getArgs() != null) {
            dto.setArgs(new LinkedHashMap<>(start.getArgs()));
        }
        return dto;
    }

    private static ChatToolCallDTO toToolEnd(ToolCallEndEvent end, Map<String, String> toolLabels) {
        ChatToolCallDTO dto = new ChatToolCallDTO();
        dto.setCallId(end.getCallId());
        dto.setPhase("end");
        dto.setToolName(end.getToolName());
        dto.setToolLabel(toolLabels.getOrDefault(end.getToolName(), end.getToolName()));
        if (end.getArgs() != null) {
            dto.setArgs(new LinkedHashMap<>(end.getArgs()));
        }
        dto.setDurationMs(end.getDurationMs());
        if (end.getError() != null) {
            dto.setError(end.getError().getMessage() == null
                    ? end.getError().getClass().getSimpleName()
                    : end.getError().getMessage());
        }
        String result = end.getText();
        if (result != null && !result.isBlank()) {
            if (result.length() > TOOL_RESULT_LIMIT) {
                result = result.substring(0, TOOL_RESULT_LIMIT) + "\n…(已截断)";
            }
            dto.setResult(result);
            dto.setLinks(extractLinks(result));
        }
        return dto;
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

    private static List<String> extractLinks(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> links = new LinkedHashSet<>();
        Matcher m = URL_PATTERN.matcher(text);
        while (m.find() && links.size() < 30) {
            String url = m.group();
            while (url.endsWith(".") || url.endsWith(",") || url.endsWith(";") || url.endsWith(")")) {
                url = url.substring(0, url.length() - 1);
            }
            if (!url.isBlank()) {
                links.add(url);
            }
        }
        return new ArrayList<>(links);
    }

    private static void emitDelta(Consumer<String> onDelta, String text) {
        if (onDelta != null && text != null && !text.isEmpty()) {
            onDelta.accept(text);
        }
    }

    private static void emitTool(Consumer<ChatToolCallDTO> onTool, ChatToolCallDTO dto) {
        if (onTool != null && dto != null) {
            onTool.accept(dto);
        }
    }

    private static void emitStatus(Consumer<String> onStatus, String text) {
        if (onStatus != null && text != null && !text.isBlank()) {
            onStatus.accept(text);
        }
    }

    private static void emitThinking(Consumer<String> onThinking, String text) {
        if (onThinking != null && text != null && !text.isEmpty()) {
            onThinking.accept(text);
        }
    }

    private static void resetPhaseFlags(boolean[] phaseFlags) {
        if (phaseFlags == null) {
            return;
        }
        if (phaseFlags.length > 0) {
            phaseFlags[0] = false;
        }
        if (phaseFlags.length > 1) {
            phaseFlags[1] = false;
        }
    }

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
