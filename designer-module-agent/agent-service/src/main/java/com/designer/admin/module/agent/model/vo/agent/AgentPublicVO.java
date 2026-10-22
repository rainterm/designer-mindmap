package com.designer.admin.module.agent.model.vo.agent;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 公开嵌入页可见的智能体信息（不含内部配置）。
 *
 * @author 雨夜终端
 */
@Data
public class AgentPublicVO {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("简介")
    private String remark;

    @ApiModelProperty("是否支持文件上传：1是 0否")
    private Integer fileEnabled;
}
