package com.designer.admin.module.agent.model.vo.session;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 *
 * @author 雨夜终端
 */
@Data
public class SessionVO {

    @ApiModelProperty("主键 ID")
    private Long id;

    @ApiModelProperty("智能体 ID")
    private Long agentId;

    @ApiModelProperty("智能体名称")
    private String agentName;

    @ApiModelProperty("智能体图标")
    private String agentAvatar;

    @ApiModelProperty("智能体是否支持文件上传")
    private Integer fileEnabled;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("消息条数")
    private Integer messageCount;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("最后消息时间")
    private LocalDateTime lastMessageAt;

    @ApiModelProperty("状态：1启用 0停用")
    private Integer status;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
