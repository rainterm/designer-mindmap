package com.designer.admin.module.agent.workflow.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeMapper;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledge;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeRetrieveService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Map;

import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.applyVars;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.doubleVal;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.intVal;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.longVal;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.parseCfg;
import static com.designer.admin.module.agent.workflow.support.WorkflowVarSupport.str;
/**
 * 知识库检索活动节点执行。
 *
 * @author 雨夜终端
 */
@Component
public class WorkflowKnowledgeActivity {

    @Db
    private AgentKnowledgeMapper knowledgeMapper;
    @Inject
    private AgentKnowledgeRetrieveService knowledgeRetrieveService;

    public String runKnowledge(String config, Map<String, String> vars) {
        Map<String, Object> cfg = parseCfg(config);
        Long knowledgeId = longVal(cfg.get("knowledgeId"));
        String code = str(cfg.get("knowledgeCode"), "");
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

        String queryTpl = str(cfg.get("query"), "");
        String queryResolved = applyVars(queryTpl, vars).trim();
        int topK = intVal(cfg.get("topK"), kb.getTopK() == null ? 5 : kb.getTopK());
        if (topK < 1) {
            topK = 1;
        }
        if (topK > 20) {
            topK = 20;
        }
        Double scoreThreshold = null;
        if (cfg.get("scoreThreshold") != null) {
            double t = doubleVal(cfg.get("scoreThreshold"), Double.NaN);
            if (!Double.isNaN(t)) {
                if (t < 0) {
                    t = 0;
                }
                if (t > 1) {
                    t = 1;
                }
                scoreThreshold = t;
            }
        }
        // 工作流侧只按 Top K 控制篇幅，不再按字符截断
        return knowledgeRetrieveService.retrieveOne(kb, queryResolved, topK, null, scoreThreshold);
    }
}
