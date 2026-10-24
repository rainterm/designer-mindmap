package com.designer.admin.module.agent.workflow.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeMapper;
import com.designer.admin.module.agent.model.dto.workflow.node.KnowledgeNodeConfigDTO;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledge;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeRetrieveService;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Map;

import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.applyVars;

/**
 * 知识库检索活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowKnowledgeActivity {

    /** 知识库元数据访问 */
    @Db
    private AgentKnowledgeMapper knowledgeMapper;
    /** 单库检索服务（Top K / 分数阈值） */
    @Inject
    private AgentKnowledgeRetrieveService knowledgeRetrieveService;

    /**
     * 执行知识库检索：按 ID 或编码定位已启用知识库，变量替换查询词后按 Top K / 阈值召回。
     *
     * @param config 节点 JSON 配置（knowledgeId / knowledgeCode / query / topK / scoreThreshold）
     * @param vars   工作流变量表，用于替换 query 模板
     * @return 检索结果文本（仅按 Top K 控制篇幅，不做字符截断）
     */
    public String runKnowledge(String config, Map<String, String> vars) {
        KnowledgeNodeConfigDTO cfg = AgentJsonUtil.parse(config, KnowledgeNodeConfigDTO.class);
        if (cfg == null) {
            cfg = new KnowledgeNodeConfigDTO();
        }
        Long knowledgeId = cfg.getKnowledgeId();
        String code = cfg.getKnowledgeCode() == null ? "" : cfg.getKnowledgeCode();
        if (knowledgeId == null && !code.isBlank()) {
            AgentKnowledge kb = knowledgeMapper.selectOne(new LambdaQueryWrapper<AgentKnowledge>()
                    .eq(AgentKnowledge::getCode, code)
                    .last("LIMIT 1"));
            if (kb != null) {
                knowledgeId = kb.getId();
            }
        }
        if (knowledgeId == null) {
            throw new BizException("知识库检索节点未选择知识库");
        }
        AgentKnowledge kb = knowledgeMapper.selectById(knowledgeId);
        if (kb == null || (kb.getStatus() != null && kb.getStatus() != SystemConsts.STATUS_ENABLE)) {
            throw new BizException("知识库不存在或未启用");
        }

        String queryTpl = cfg.getQuery() == null ? "" : cfg.getQuery();
        String queryResolved = applyVars(queryTpl, vars).trim();
        int topK = cfg.getTopK() == null ? (kb.getTopK() == null ? 5 : kb.getTopK()) : cfg.getTopK();
        if (topK < 1) {
            topK = 1;
        }
        if (topK > 20) {
            topK = 20;
        }
        Double scoreThreshold = null;
        if (cfg.getScoreThreshold() != null) {
            double t = cfg.getScoreThreshold();
            if (t < 0) {
                t = 0;
            }
            if (t > 1) {
                t = 1;
            }
            scoreThreshold = t;
        }
        // 工作流侧只按 Top K 控制篇幅，不再按字符截断
        return knowledgeRetrieveService.retrieveOne(kb, queryResolved, topK, null, scoreThreshold);
    }
}
