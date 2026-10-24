package com.designer.admin.module.agent.service.session.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;
import org.noear.solon.web.sse.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.satoken.OperatorHelper;
import com.designer.admin.module.agent.constant.session.AgentMessageRole;
import com.designer.admin.module.agent.constant.session.AgentSseEvent;
import com.designer.admin.module.agent.harness.context.MmProjectChatContext;
import com.designer.admin.module.agent.util.AgentAsyncExecutor;
import com.designer.admin.module.agent.mapper.agent.AgentAgentMapper;
import com.designer.admin.module.agent.mapper.session.AgentMessageMapper;
import com.designer.admin.module.agent.mapper.session.AgentSessionMapper;
import com.designer.admin.module.agent.model.dto.session.ChatAttachmentDTO;
import com.designer.admin.module.agent.model.dto.session.ChatStreamResult;
import com.designer.admin.module.agent.model.dto.session.ChatToolCallDTO;
import com.designer.admin.module.agent.model.dto.session.SendMessageDTO;
import com.designer.admin.module.agent.model.dto.session.SessionQueryDTO;
import com.designer.admin.module.agent.model.dto.session.SessionSaveDTO;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.session.AgentMessage;
import com.designer.admin.module.agent.model.entity.session.AgentSession;
import com.designer.admin.module.agent.model.vo.session.ChatRecordVO;
import com.designer.admin.module.agent.model.vo.session.MessageVO;
import com.designer.admin.module.agent.model.vo.session.SendMessageResult;
import com.designer.admin.module.agent.model.vo.session.SessionVO;
import com.designer.admin.module.agent.service.agent.AgentAgentService;
import com.designer.admin.module.agent.service.session.AgentChatService;
import com.designer.admin.module.agent.service.session.AgentSessionService;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import com.designer.admin.module.agent.util.AgentSseUtil;
import org.noear.solon.Utils;

/**
 * 智能体会话
 *
 * @author 雨夜终端
 */
@Component
public class AgentSessionServiceImpl implements AgentSessionService {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionServiceImpl.class);

    @Db
    private AgentSessionMapper sessionMapper;
    @Db
    private AgentMessageMapper messageMapper;
    @Db
    private AgentAgentMapper agentMapper;
    @Inject
    private AgentChatService chatService;
    @Inject
    private AgentAgentService agentService;
    @Inject
    private AgentAsyncExecutor asyncExecutor;

    @Override
    public IPage<ChatRecordVO> page(SessionQueryDTO query) {
        List<ChatRecordVO> all = buildChatRecords();
        String kw = firstNonBlank(query.getTitle(), query.getKeyword());
        if (Utils.isNotBlank(kw)) {
            String needle = kw.trim().toLowerCase();
            all = all.stream()
                    .filter(r -> contains(r.getQuestion(), needle) || contains(r.getAnswer(), needle))
                    .collect(Collectors.toList());
        }
        if (query.getAgentId() != null) {
            all = all.stream()
                    .filter(r -> Objects.equals(r.getAgentId(), query.getAgentId()))
                    .collect(Collectors.toList());
        }
        if (query.getStatus() != null) {
            all = all.stream()
                    .filter(r -> Objects.equals(r.getStatus(), query.getStatus()))
                    .collect(Collectors.toList());
        }
        long pageNum = query.getPageNum();
        long pageSize = query.getPageSize();
        long total = all.size();
        int from = (int) Math.min((pageNum - 1) * pageSize, total);
        int to = (int) Math.min(from + pageSize, total);
        List<ChatRecordVO> records = all.subList(from, to);
        Page<ChatRecordVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records);
        return page;
    }

    @Override
    public SessionVO getById(Long id) {
        return toSessionVo(require(id));
    }

    @Override
    public List<SessionVO> listMineByAgent(Long agentId) {
        return listByOwnerEntities(agentId, OperatorHelper.currentOperator()).stream()
                .map(this::toSessionVo)
                .toList();
    }

    @Override
    public List<SessionVO> listByOwner(Long agentId, String owner) {
        return listByOwnerEntities(agentId, owner).stream().map(this::toSessionVo).toList();
    }

    @Override
    public SessionVO openMine(Long agentId) {
        return openByOwner(agentId, OperatorHelper.currentOperator());
    }

    @Override
    public SessionVO openByOwner(Long agentId, String owner) {
        List<AgentSession> mine = listByOwnerEntities(agentId, owner);
        AgentSession withMessages = null;
        AgentSession empty = null;
        for (AgentSession s : mine) {
            if (isDebugSessionTitle(s.getTitle())) {
                continue;
            }
            int count = s.getMessageCount() == null ? 0 : s.getMessageCount();
            if (count > 0) {
                if (withMessages == null) {
                    withMessages = s;
                }
            } else if (empty == null) {
                empty = s;
            }
        }
        if (withMessages != null) {
            return toSessionVo(withMessages);
        }
        if (empty != null) {
            return toSessionVo(empty);
        }
        SessionSaveDTO dto = new SessionSaveDTO();
        dto.setAgentId(agentId);
        dto.setTitle("新会话");
        return create(dto, owner);
    }

    private List<AgentSession> listByOwnerEntities(Long agentId, String owner) {
        if (agentId == null) {
            throw new BizException("智能体ID不能为空");
        }
        if (Utils.isBlank(owner)) {
            return List.of();
        }
        return sessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getAgentId, agentId)
                .eq(AgentSession::getCreateBy, owner.trim())
                .orderByDesc(AgentSession::getLastMessageAt)
                .orderByDesc(AgentSession::getId));
    }

    /** 调试面板创建的会话（标题「调试」）不参与商店打开复用 */
    private static boolean isDebugSessionTitle(String title) {
        if (title == null) {
            return false;
        }
        String t = title.trim();
        return "调试".equals(t) || t.startsWith("调试 ·") || t.startsWith("调试·");
    }

    @Override
    public SessionVO create(SessionSaveDTO dto) {
        return create(dto, null);
    }

    @Override
    public SessionVO create(SessionSaveDTO dto, String owner) {
        if (dto.getAgentId() == null) {
            throw new BizException("智能体ID不能为空");
        }
        AgentAgent agent = agentMapper.selectById(dto.getAgentId());
        if (agent == null) {
            throw new BizException("智能体不存在");
        }
        AgentSession session = new AgentSession();
        session.setAgentId(dto.getAgentId());
        session.setTitle(Utils.isNotBlank(dto.getTitle()) ? dto.getTitle().trim() : "新会话");
        session.setMessageCount(0);
        session.setLastMessageAt(LocalDateTime.now());
        session.setStatus(SystemConsts.STATUS_ENABLE);
        if (Utils.isNotBlank(owner)) {
            session.setCreateBy(owner.trim());
            session.setUpdateBy(owner.trim());
        }
        sessionMapper.insert(session);
        return toSessionVo(session);
    }

    @Override
    public SessionVO requireOwned(Long sessionId, String owner) {
        AgentSession session = require(sessionId);
        if (Utils.isBlank(owner) || !owner.trim().equals(session.getCreateBy())) {
            throw new BizException("会话不存在或无权访问");
        }
        return toSessionVo(session);
    }

    @Override
    @Transaction
    public void removeSessions(List<Long> ids) {
        removeSessionsByOwner(ids, OperatorHelper.currentOperator());
    }

    @Override
    @Transaction
    public void removeSessionsByOwner(List<Long> ids, String owner) {
        if (ids == null || ids.isEmpty() || Utils.isBlank(owner)) {
            return;
        }
        List<AgentSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .in(AgentSession::getId, ids)
                .eq(AgentSession::getCreateBy, owner.trim()));
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        List<Long> sessionIds = sessions.stream().map(AgentSession::getId).toList();
        messageMapper.delete(new LambdaQueryWrapper<AgentMessage>()
                .in(AgentMessage::getSessionId, sessionIds));
        sessionMapper.deleteBatchIds(sessionIds);
    }

    @Override
    @Transaction
    public void removeByUserMessageIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<Long> idSet = new HashSet<>(ids);
        List<AgentMessage> users = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .in(AgentMessage::getId, ids)
                .eq(AgentMessage::getRole, AgentMessageRole.USER.getCode()));
        Set<Long> sessionIds = users.stream()
                .map(AgentMessage::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Long sid : sessionIds) {
            List<AgentMessage> msgs = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                    .eq(AgentMessage::getSessionId, sid)
                    .orderByAsc(AgentMessage::getId));
            List<Long> deleteIds = new ArrayList<>();
            for (int i = 0; i < msgs.size(); i++) {
                AgentMessage m = msgs.get(i);
                if (AgentMessageRole.USER.eq(m.getRole()) && idSet.contains(m.getId())) {
                    deleteIds.add(m.getId());
                    if (i + 1 < msgs.size() && AgentMessageRole.ASSISTANT.eq(msgs.get(i + 1).getRole())) {
                        deleteIds.add(msgs.get(i + 1).getId());
                        i++;
                    }
                }
            }
            if (!deleteIds.isEmpty()) {
                messageMapper.deleteBatchIds(deleteIds);
            }
            refreshSessionStats(sid);
        }
    }

    @Override
    public List<MessageVO> listMessages(Long sessionId) {
        require(sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
                        .orderByAsc(AgentMessage::getId))
                .stream()
                .map(this::toMessageVo)
                .toList();
    }

    @Override
    public SendMessageResult send(Long sessionId, SendMessageDTO dto) {
        AgentSession session = require(sessionId);
        AgentAgent agent = resolveRuntimeAgent(session.getAgentId(), dto);
        String content = dto == null || dto.getContent() == null ? "" : dto.getContent();
        List<ChatAttachmentDTO> attachments = normalizeAttachments(agent, dto == null ? null : dto.getAttachments());
        LocalDateTime now = LocalDateTime.now();

        AgentMessage userMsg = new AgentMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole(AgentMessageRole.USER.getCode());
        userMsg.setContent(content);
        userMsg.setAttachmentsJson(attachments.isEmpty() ? null : AgentJsonUtil.toJson(stripHeavy(attachments)));
        userMsg.setCreatedAt(now);
        messageMapper.insert(userMsg);

        String reply;
        Long mmProjectId = dto == null ? null : dto.getMmProjectId();
        try {
            MmProjectChatContext.setProjectId(mmProjectId);
            reply = chatService.chat(session, agent, content, attachments);
        } catch (Exception e) {
            reply = "对话失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            MmProjectChatContext.clear();
        }

        AgentMessage assistantMsg = new AgentMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole(AgentMessageRole.ASSISTANT.getCode());
        assistantMsg.setContent(reply == null ? "" : reply);
        assistantMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(assistantMsg);
        touchSession(session, titleSeed(content, attachments), assistantMsg);

        SendMessageResult result = new SendMessageResult();
        result.setUser(toMessageVo(userMsg));
        result.setAssistant(toMessageVo(assistantMsg));
        return result;
    }

    @Override
    public SseEmitter sendStream(Long sessionId, SendMessageDTO dto) {
        AgentSession session = require(sessionId);
        AgentAgent agent = resolveRuntimeAgent(session.getAgentId(), dto);
        String content = dto == null || dto.getContent() == null ? "" : dto.getContent();
        List<ChatAttachmentDTO> attachments = normalizeAttachments(agent, dto == null ? null : dto.getAttachments());
        Long mmProjectId = dto == null ? null : dto.getMmProjectId();

        AgentMessage userMsg = new AgentMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole(AgentMessageRole.USER.getCode());
        userMsg.setContent(content);
        userMsg.setAttachmentsJson(attachments.isEmpty() ? null : AgentJsonUtil.toJson(stripHeavy(attachments)));
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        final SseEmitter[] box = new SseEmitter[1];
        AtomicBoolean closed = new AtomicBoolean(false);
        SseEmitter emitter = new SseEmitter(-1L)
                .onCompletion(() -> closed.set(true))
                .onError(e -> closed.set(true))
                .onInited(s -> {
                    box[0] = s;
                    asyncExecutor.execute(
                            () -> runStreamChat(s, session, agent, content, attachments, userMsg, closed, mmProjectId));
                });
        box[0] = emitter;
        return emitter;
    }

    /**
     * 解析本次对话使用的智能体配置：
     * useDraft=true（调试面板）时叠加草稿配置（draft_json），其余走已发布正式字段。
     * 草稿仅覆盖内存副本，不写库、不影响已发布版本。
     */
    private AgentAgent resolveRuntimeAgent(Long agentId, SendMessageDTO dto) {
        AgentAgent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException("智能体不存在");
        }
        if (dto != null && Boolean.TRUE.equals(dto.getUseDraft())) {
            agentService.applyDraftConfig(agent);
        }
        return agent;
    }

    private void runStreamChat(SseEmitter emitter, AgentSession session, AgentAgent agent,
                               String content, List<ChatAttachmentDTO> attachments,
                               AgentMessage userMsg, AtomicBoolean closed, Long mmProjectId) {
        MmProjectChatContext.setProjectId(mmProjectId);
        try {
            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.USER.getCode(), JsonUtil.toJson(toMessageVo(userMsg)));

            String reply;
            List<ChatToolCallDTO> tools = List.of();
            try {
                StringBuilder thinkingBuf = new StringBuilder();
                ChatStreamResult streamResult = chatService.chatStream(
                        session,
                        agent,
                        content,
                        attachments,
                        delta -> {
                            Map<String, Object> payload = new LinkedHashMap<>();
                            payload.put("delta", delta);
                            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.DELTA.getCode(), JsonUtil.toJson(payload));
                        },
                        tool -> AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.TOOL.getCode(), JsonUtil.toJson(tool)),
                        status -> {
                            Map<String, Object> payload = new LinkedHashMap<>();
                            payload.put("status", status);
                            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.STATUS.getCode(), JsonUtil.toJson(payload));
                        },
                        thinking -> {
                            if (thinking != null && !thinking.isEmpty()) {
                                thinkingBuf.append(thinking);
                            }
                            Map<String, Object> payload = new LinkedHashMap<>();
                            payload.put("delta", thinking == null ? "" : thinking);
                            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.THINKING.getCode(), JsonUtil.toJson(payload));
                        });
                reply = streamResult.getContent();
                tools = streamResult.getTools() == null ? List.of() : streamResult.getTools();
                String thinkingText = streamResult.getThinking();
                if (thinkingText == null || thinkingText.isBlank()) {
                    thinkingText = thinkingBuf.toString();
                }
                AgentMessage assistantMsg = new AgentMessage();
                assistantMsg.setSessionId(session.getId());
                assistantMsg.setRole(AgentMessageRole.ASSISTANT.getCode());
                assistantMsg.setContent(reply == null ? "" : reply);
                assistantMsg.setToolsJson(tools.isEmpty() ? null : AgentJsonUtil.toJson(tools));
                assistantMsg.setThinking(thinkingText == null || thinkingText.isBlank() ? null : thinkingText);
                assistantMsg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(assistantMsg);
                touchSession(session, titleSeed(content, attachments), assistantMsg);

                SendMessageResult result = new SendMessageResult();
                result.setUser(toMessageVo(userMsg));
                result.setAssistant(toMessageVo(assistantMsg));
                AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.DONE.getCode(), JsonUtil.toJson(result));
                return;
            } catch (Exception e) {
                reply = "对话失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("msg", reply);
                AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.ERROR.getCode(), JsonUtil.toJson(err));
            }

            AgentMessage assistantMsg = new AgentMessage();
            assistantMsg.setSessionId(session.getId());
            assistantMsg.setRole(AgentMessageRole.ASSISTANT.getCode());
            assistantMsg.setContent(reply == null ? "" : reply);
            assistantMsg.setToolsJson(tools.isEmpty() ? null : AgentJsonUtil.toJson(tools));
            assistantMsg.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(assistantMsg);
            touchSession(session, titleSeed(content, attachments), assistantMsg);

            SendMessageResult result = new SendMessageResult();
            result.setUser(toMessageVo(userMsg));
            result.setAssistant(toMessageVo(assistantMsg));
            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.DONE.getCode(), JsonUtil.toJson(result));
        } catch (Exception e) {
            log.error("流式对话失败 sessionId={}", session.getId(), e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("msg", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.ERROR.getCode(), JsonUtil.toJson(err));
        } finally {
            MmProjectChatContext.clear();
            if (!closed.get()) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    private void touchSession(AgentSession session, String content, AgentMessage assistantMsg) {
        Long nonSystem = messageMapper.selectCount(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, session.getId())
                .ne(AgentMessage::getRole, AgentMessageRole.SYSTEM.getCode()));
        session.setMessageCount(nonSystem == null ? 0 : nonSystem.intValue());
        session.setLastMessageAt(assistantMsg.getCreatedAt());
        // 标题以首条用户提问为准；占位标题在首次对话后改写
        if (isPlaceholderTitle(session.getTitle())) {
            String firstQ = firstUserQuestion(session.getId());
            String src = Utils.isNotBlank(firstQ) ? firstQ : content;
            session.setTitle(toSessionTitle(src));
        }
        sessionMapper.updateById(session);
    }

    private String firstUserQuestion(Long sessionId) {
        AgentMessage first = messageMapper.selectOne(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .eq(AgentMessage::getRole, AgentMessageRole.USER.getCode())
                .orderByAsc(AgentMessage::getId)
                .last("LIMIT 1"));
        return first == null ? null : first.getContent();
    }

    private static boolean isPlaceholderTitle(String title) {
        if (Utils.isBlank(title) || "新会话".equals(title)) {
            return true;
        }
        return title.startsWith("对话 ·") || title.startsWith("调试 ·");
    }

    private static String toSessionTitle(String content) {
        if (Utils.isBlank(content)) {
            return "新会话";
        }
        String t = content.trim().replaceAll("\\s+", " ");
        return t.length() > 24 ? t.substring(0, 24) : t;
    }

    private List<ChatRecordVO> buildChatRecords() {
        List<AgentSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
                .orderByDesc(AgentSession::getId));
        Map<Long, String> agentNames = loadAgentNames(sessions);
        List<ChatRecordVO> records = new ArrayList<>();
        for (AgentSession session : sessions) {
            List<AgentMessage> msgs = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                    .eq(AgentMessage::getSessionId, session.getId())
                    .orderByAsc(AgentMessage::getId));
            for (int i = 0; i < msgs.size(); i++) {
                AgentMessage m = msgs.get(i);
                if (!AgentMessageRole.USER.eq(m.getRole())) {
                    continue;
                }
                String answer = "";
                if (i + 1 < msgs.size() && AgentMessageRole.ASSISTANT.eq(msgs.get(i + 1).getRole())) {
                    answer = msgs.get(i + 1).getContent() == null ? "" : msgs.get(i + 1).getContent();
                }
                ChatRecordVO vo = new ChatRecordVO();
                vo.setId(m.getId());
                vo.setSessionId(session.getId());
                vo.setAgentId(session.getAgentId());
                vo.setAgentName(agentNames.getOrDefault(session.getAgentId(), ""));
                vo.setQuestion(m.getContent() == null ? "" : m.getContent());
                vo.setAnswer(answer);
                vo.setCreatedAt(m.getCreatedAt());
                vo.setStatus(session.getStatus());
                records.add(vo);
            }
        }
        records.sort(Comparator.comparing(ChatRecordVO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return records;
    }

    private void refreshSessionStats(Long sessionId) {
        AgentSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        List<AgentMessage> msgs = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getId));
        int count = (int) msgs.stream().filter(m -> !AgentMessageRole.SYSTEM.eq(m.getRole())).count();
        session.setMessageCount(count);
        AgentMessage last = null;
        for (int i = msgs.size() - 1; i >= 0; i--) {
            if (!AgentMessageRole.SYSTEM.eq(msgs.get(i).getRole())) {
                last = msgs.get(i);
                break;
            }
        }
        session.setLastMessageAt(last != null ? last.getCreatedAt() : session.getCreateTime());
        sessionMapper.updateById(session);
    }

    private AgentSession require(Long id) {
        AgentSession row = sessionMapper.selectById(id);
        if (row == null) {
            throw new BizException("会话不存在");
        }
        return row;
    }

    private SessionVO toSessionVo(AgentSession e) {
        SessionVO vo = new SessionVO();
        vo.setId(e.getId());
        vo.setAgentId(e.getAgentId());
        vo.setTitle(e.getTitle());
        vo.setMessageCount(e.getMessageCount());
        vo.setLastMessageAt(e.getLastMessageAt());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        if (e.getAgentId() != null) {
            AgentAgent agent = agentMapper.selectById(e.getAgentId());
            vo.setAgentName(agent == null ? "" : agent.getName());
            vo.setAgentAvatar(agent == null ? "" : agent.getAvatar());
            vo.setFileEnabled(agent == null || agent.getFileEnabled() == null ? 0 : agent.getFileEnabled());
        } else {
            vo.setAgentName("");
            vo.setAgentAvatar("");
            vo.setFileEnabled(0);
        }
        return vo;
    }

    private MessageVO toMessageVo(AgentMessage m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setSessionId(m.getSessionId());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setAttachments(parseAttachments(m.getAttachmentsJson()));
        vo.setTools(parseTools(m.getToolsJson()));
        vo.setThinking(m.getThinking());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }

    private static List<ChatToolCallDTO> parseTools(String json) {
        List<Map<String, Object>> rows = AgentJsonUtil.parseMapList(json);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<ChatToolCallDTO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            ChatToolCallDTO t = new ChatToolCallDTO();
            t.setCallId(strVal(row.get("callId")));
            t.setPhase(strVal(row.get("phase")));
            t.setToolName(strVal(row.get("toolName")));
            t.setToolLabel(strVal(row.get("toolLabel")));
            t.setResult(strVal(row.get("result")));
            t.setError(strVal(row.get("error")));
            Object duration = row.get("durationMs");
            if (duration instanceof Number n) {
                t.setDurationMs(n.longValue());
            }
            Object args = row.get("args");
            if (args instanceof Map<?, ?> map) {
                Map<String, Object> copied = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (e.getKey() != null) {
                        copied.put(String.valueOf(e.getKey()), e.getValue());
                    }
                }
                t.setArgs(copied);
            }
            Object links = row.get("links");
            if (links instanceof List<?> linkList) {
                List<String> out = new ArrayList<>();
                for (Object item : linkList) {
                    if (item != null && !String.valueOf(item).isBlank()) {
                        out.add(String.valueOf(item));
                    }
                }
                t.setLinks(out);
            }
            list.add(t);
        }
        return list;
    }

    private List<ChatAttachmentDTO> normalizeAttachments(AgentAgent agent, List<ChatAttachmentDTO> raw) {
        if (agent == null || agent.getFileEnabled() == null || agent.getFileEnabled() != 1) {
            return List.of();
        }
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ChatAttachmentDTO> list = new ArrayList<>();
        for (ChatAttachmentDTO a : raw) {
            if (a == null || Utils.isBlank(a.getUrl())) {
                continue;
            }
            ChatAttachmentDTO row = new ChatAttachmentDTO();
            row.setName(a.getName() == null ? "" : a.getName().trim());
            row.setUrl(a.getUrl().trim());
            row.setContentType(a.getContentType() == null ? "" : a.getContentType().trim());
            row.setSize(a.getSize() == null ? 0L : a.getSize());
            if (Utils.isNotBlank(a.getTextContent())) {
                String text = a.getTextContent();
                if (text.length() > 80_000) {
                    text = text.substring(0, 80_000) + "\n…(已截断)";
                }
                row.setTextContent(text);
            }
            if (Utils.isNotBlank(a.getBase64()) && a.getBase64().length() <= 4_000_000) {
                row.setBase64(a.getBase64());
            }
            list.add(row);
            if (list.size() >= 10) {
                break;
            }
        }
        return list;
    }

    private static List<ChatAttachmentDTO> parseAttachments(String json) {
        List<Map<String, Object>> rows = AgentJsonUtil.parseMapList(json);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<ChatAttachmentDTO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            ChatAttachmentDTO a = new ChatAttachmentDTO();
            a.setName(strVal(row.get("name")));
            a.setUrl(strVal(row.get("url")));
            a.setContentType(strVal(row.get("contentType")));
            Object size = row.get("size");
            if (size instanceof Number n) {
                a.setSize(n.longValue());
            }
            // 历史消息不回传大段 textContent，避免列表膨胀
            list.add(a);
        }
        return list;
    }

    private static String strVal(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String titleSeed(String content, List<ChatAttachmentDTO> attachments) {
        if (Utils.isNotBlank(content)) {
            return content;
        }
        if (attachments != null && !attachments.isEmpty() && Utils.isNotBlank(attachments.get(0).getName())) {
            return attachments.get(0).getName();
        }
        return content;
    }

    /** 落库时去掉 base64 / 大段文本，避免消息表膨胀 */
    private static List<ChatAttachmentDTO> stripHeavy(List<ChatAttachmentDTO> attachments) {
        List<ChatAttachmentDTO> list = new ArrayList<>(attachments.size());
        for (ChatAttachmentDTO a : attachments) {
            ChatAttachmentDTO row = new ChatAttachmentDTO();
            row.setName(a.getName());
            row.setUrl(a.getUrl());
            row.setContentType(a.getContentType());
            row.setSize(a.getSize());
            list.add(row);
        }
        return list;
    }

    private Map<Long, String> loadAgentNames(List<AgentSession> sessions) {
        List<Long> ids = sessions.stream()
                .map(AgentSession::getAgentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return agentMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(AgentAgent::getId, AgentAgent::getName, (a, b) -> a, HashMap::new));
    }

    private static boolean contains(String hay, String needleLower) {
        return hay != null && hay.toLowerCase().contains(needleLower);
    }

    private static String firstNonBlank(String a, String b) {
        if (Utils.isNotBlank(a)) {
            return a;
        }
        if (Utils.isNotBlank(b)) {
            return b;
        }
        return null;
    }
}
