package com.designer.admin.module.agent.model.dto.session;

import io.swagger.annotations.ApiModelProperty;
import com.designer.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话查询参数
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SessionQueryDTO extends PageQuery {

    @ApiModelProperty("会话标题（模糊）")
    private String title;

    @ApiModelProperty("消息关键词")
    private String keyword;

    @ApiModelProperty("智能体 ID")
    private Long agentId;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;
}
