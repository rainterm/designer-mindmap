package com.designer.admin.module.agent.service.session;

import com.designer.admin.module.agent.model.dto.session.SendMessageDTO;
import com.designer.admin.module.agent.model.dto.session.SessionQueryDTO;
import com.designer.admin.module.agent.model.dto.session.SessionSaveDTO;
import com.designer.admin.module.agent.model.vo.session.ChatRecordVO;
import com.designer.admin.module.agent.model.vo.session.MessageVO;
import com.designer.admin.module.agent.model.vo.session.SendMessageResult;
import com.designer.admin.module.agent.model.vo.session.SessionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import org.noear.solon.web.sse.SseEmitter;

/**
 * 智能体会话
 *
 * @author 雨夜终端
 */
public interface AgentSessionService {

    /**
     * 分页返回扁平化聊天记录（用户消息 + 紧随助手回复）。
     *
     * @param query 分页与智能体 / 关键字筛选条件
     * @return 聊天记录分页结果
     */
    IPage<ChatRecordVO> page(SessionQueryDTO query);

    /**
     * 按主键查询会话详情。
     *
     * @param id 会话 ID
     * @return 会话详情
     */
    SessionVO getById(Long id);

    /**
     * 当前用户在某智能体下的会话列表（聊天侧栏）。
     *
     * @param agentId 智能体 ID
     * @return 会话列表
     */
    List<SessionVO> listMineByAgent(Long agentId);

    /**
     * 指定归属者在某智能体下的会话列表（开放接口 / 公开嵌入）。
     *
     * @param agentId 智能体 ID
     * @param owner   归属标识（如 {@code oauth:clientId} / {@code public:token}）
     * @return 会话列表
     */
    List<SessionVO> listByOwner(Long agentId, String owner);

    /**
     * 打开当前用户在某智能体下的会话：优先复用最近非调试会话，没有才新建。
     * 用于应用商店「使用」，避免每次点开都堆空的「新会话」。
     *
     * @param agentId 智能体 ID
     * @return 可进入的会话
     */
    SessionVO openMine(Long agentId);

    /**
     * 按归属者打开会话：优先复用最近非调试会话，没有才新建。
     *
     * @param agentId 智能体 ID
     * @param owner   归属标识
     * @return 可进入的会话
     */
    SessionVO openByOwner(Long agentId, String owner);

    /**
     * 创建会话。
     *
     * @param dto 创建参数
     * @return 新建会话
     */
    SessionVO create(SessionSaveDTO dto);

    /**
     * 以指定归属者创建会话。
     *
     * @param dto   创建参数
     * @param owner 归属标识；空则走自动填充
     * @return 新建会话
     */
    SessionVO create(SessionSaveDTO dto, String owner);

    /**
     * 校验会话属于指定归属者。
     *
     * @param sessionId 会话 ID
     * @param owner     归属标识
     * @return 会话详情
     */
    SessionVO requireOwned(Long sessionId, String owner);

    /**
     * 删除整段会话（仅当前用户自己的会话）及其消息。
     *
     * @param ids 会话 ID 列表
     */
    void removeSessions(List<Long> ids);

    /**
     * 按指定归属者删除会话及其消息。
     *
     * @param ids   会话 ID 列表
     * @param owner 归属标识
     */
    void removeSessionsByOwner(List<Long> ids, String owner);

    /**
     * 按用户消息 id 删除：移除用户消息及紧随的助手消息。
     *
     * @param ids 用户消息 ID 列表
     */
    void removeByUserMessageIds(List<Long> ids);

    /**
     * 列出会话下全部消息。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<MessageVO> listMessages(Long sessionId);

    /**
     * 同步发送消息并返回助手回复。
     *
     * @param sessionId 会话 ID
     * @param dto       消息内容与附件
     * @return 发送结果（含助手回复）
     */
    SendMessageResult send(Long sessionId, SendMessageDTO dto);

    /**
     * SSE 流式发送：event = user / delta / tool / done / error。
     *
     * @param sessionId 会话 ID
     * @param dto       消息内容与附件
     * @return SSE 发射器
     */
    SseEmitter sendStream(Long sessionId, SendMessageDTO dto);

}
