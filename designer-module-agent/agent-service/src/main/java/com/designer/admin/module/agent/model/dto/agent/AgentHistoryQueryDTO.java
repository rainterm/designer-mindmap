package com.designer.admin.module.agent.model.dto.agent;

import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体历史版本分页查询参数（仅承接 pageNum/pageSize）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentHistoryQueryDTO extends PageQuery {

}
