package com.designer.admin.system.model.dto.message;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息通道配置分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageConfigQueryDTO extends PageQuery {

    @ApiModelProperty("配置名称，模糊匹配")
    private String configName;

    @ApiModelProperty("通道类型，如 email / inbox")
    private String channelType;

    @ApiModelProperty("状态：1启用 0停用，为空表示不限")
    private Integer status;
}
