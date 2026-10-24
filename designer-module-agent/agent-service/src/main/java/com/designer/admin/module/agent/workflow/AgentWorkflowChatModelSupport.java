package com.designer.admin.module.agent.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.harness.config.AgentChatConfigFactory;
import com.designer.admin.module.agent.mapper.agent.AgentAgentMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.model.entity.agent.AgentAgent;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.ai.chat.ChatConfig;
import org.noear.solon.ai.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工作流侧 ChatModel 解析：按智能体绑定，或回退到默认启用智能体。
 *
 * @author 雨夜终端
 */
@Component
public class AgentWorkflowChatModelSupport {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowChatModelSupport.class);

    /** 智能体 Mapper */
    @Db
    private AgentAgentMapper agentMapper;
    /** 模型供应商 Mapper */
    @Db
    private AgentProviderMapper providerMapper;

    /**
     * 按智能体 id 解析 ChatModel；未配置供应商/模型时返回 null。
     *
     * @param agentId 智能体 id；为 null 时返回 null
     * @return ChatModel；未配置时 null
     * @throws BizException 智能体不存在时抛出
     */
    public ChatModel resolveChatModel(Long agentId) {
        if (agentId == null) {
            return null;
        }
        AgentAgent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException("智能体不存在");
        }
        // 纯数据流工作流可能无模型：未配置时不抛错，留给 LLM 节点执行时再回退默认
        if (agent.getProviderId() == null || agent.getModelCode() == null || agent.getModelCode().isBlank()) {
            return null;
        }
        return buildChatModel(agent);
    }

    /**
     * 取第一个已启用且配置了供应商/模型的智能体，构建默认 ChatModel。
     *
     * @return ChatModel；无可用智能体或构建失败时 null
     */
    public ChatModel resolveDefaultChatModel() {
        AgentAgent agent = agentMapper.selectOne(new LambdaQueryWrapper<AgentAgent>()
                .eq(AgentAgent::getStatus, SystemConsts.STATUS_ENABLE)
                .isNotNull(AgentAgent::getProviderId)
                .isNotNull(AgentAgent::getModelCode)
                .ne(AgentAgent::getModelCode, "")
                .orderByAsc(AgentAgent::getId)
                .last("LIMIT 1"));
        if (agent == null) {
            return null;
        }
        try {
            return buildChatModel(agent);
        } catch (BizException e) {
            log.warn("默认智能体模型不可用", e);
            return null;
        }
    }

    /**
     * 根据智能体绑定的供应商与模型构建 ChatModel。
     *
     * @param agent 智能体实体
     * @return ChatModel
     * @throws BizException 未配置或供应商不可用时抛出
     */
    public ChatModel buildChatModel(AgentAgent agent) {
        if (agent.getProviderId() == null || agent.getModelCode() == null || agent.getModelCode().isBlank()) {
            throw new BizException("智能体未配置供应商或模型");
        }
        AgentProvider provider = providerMapper.selectById(agent.getProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            throw new BizException("模型供应商不可用");
        }
        ChatConfig config = AgentChatConfigFactory.fromProvider(
                provider.getType(), provider.getBaseUrl(), provider.getApiKey(), agent.getModelCode());
        return config.toChatModel();
    }
}
