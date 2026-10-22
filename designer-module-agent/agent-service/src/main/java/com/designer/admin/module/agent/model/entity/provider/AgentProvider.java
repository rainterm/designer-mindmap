package com.designer.admin.module.agent.model.entity.provider;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型供应商实体
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_provider")
public class AgentProvider extends BaseEntity {

    /** 名称 */
    private String name;

    /** Solon 聊天方言：openai / ollama / anthropic / gemini / dashscope 等 */
    private String type;

    /** API Base URL */
    private String baseUrl;

    /** API Key（明文存储） */
    private String apiKey;

    /** API Key 脱敏展示 */
    private String apiKeyMasked;

    /** 模型列表 JSON */
    private String modelsJson;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
