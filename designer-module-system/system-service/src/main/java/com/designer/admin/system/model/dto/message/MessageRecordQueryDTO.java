package com.designer.admin.system.model.dto.message;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息发送记录分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageRecordQueryDTO extends PageQuery {

    @ApiModelProperty("通道类型，如 email / inbox")
    private String channelType;

    @ApiModelProperty("消息标题，模糊匹配")
    private String title;

    @ApiModelProperty("接收人，模糊匹配")
    private String receivers;

    @ApiModelProperty("发送人，模糊匹配")
    private String sendBy;

    @ApiModelProperty("发送状态，为空表示不限")
    private Integer status;
}
