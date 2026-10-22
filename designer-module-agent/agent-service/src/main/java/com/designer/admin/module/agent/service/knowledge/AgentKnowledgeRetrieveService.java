package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledge;
import java.util.List;

/**
 * 知识库检索
 *
 * @author 雨夜终端
 */
public interface AgentKnowledgeRetrieveService {

    /**
     * 多知识库检索并拼接为上下文字符串（默认检索选项）。
     *
     * @param knowledgeIds 知识库 ID 列表
     * @param query        查询文本
     * @param maxChars     拼接结果最大字符数
     * @return 检索上下文字符串，无命中时可为空串
     */
    String retrieveBlock(List<Long> knowledgeIds, String query, int maxChars);

    /**
     * 多知识库检索并拼接为上下文字符串。
     *
     * @param knowledgeIds 知识库 ID 列表
     * @param query        查询文本
     * @param maxChars     拼接结果最大字符数
     * @param options      检索选项（TopK、阈值等），可空
     * @return 检索上下文字符串，无命中时可为空串
     */
    String retrieveBlock(List<Long> knowledgeIds, String query, int maxChars, RagRetrieveOptions options);

    /**
     * 多知识库检索并返回结构化结果（命中分块、分数等）。
     *
     * @param knowledgeIds 知识库 ID 列表
     * @param query        查询文本
     * @param maxChars     拼接结果最大字符数
     * @param options      检索选项，可空
     * @return 结构化检索结果
     */
    RagRetrieveResult retrieveDetailed(List<Long> knowledgeIds, String query, int maxChars, RagRetrieveOptions options);

    /**
     * 单知识库检索并拼接为上下文字符串（无分数阈值）。
     *
     * @param kb            知识库实体
     * @param query         查询文本
     * @param topkOverride  TopK 覆盖，可空则用知识库配置
     * @param maxChars      拼接结果最大字符数，可空
     * @return 检索上下文字符串
     */
    String retrieveOne(AgentKnowledge kb, String query, Integer topkOverride, Integer maxChars);

    /**
     * 单知识库检索并拼接为上下文字符串。
     *
     * @param kb             知识库实体
     * @param query          查询文本
     * @param topkOverride   TopK 覆盖，可空则用知识库配置
     * @param maxChars       拼接结果最大字符数，可空
     * @param scoreThreshold 分数阈值，可空
     * @return 检索上下文字符串
     */
    String retrieveOne(AgentKnowledge kb, String query, Integer topkOverride, Integer maxChars, Double scoreThreshold);

}
