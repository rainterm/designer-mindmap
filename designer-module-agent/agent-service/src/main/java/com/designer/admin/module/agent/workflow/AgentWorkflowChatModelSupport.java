package com.designer.admin.module.agent.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.harness.AgentChatConfigFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowChatModelSupport.class);

    @Db
    private AgentAgentMapper agentMapper;
    @Db
    private AgentProviderMapper providerMapper;

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
