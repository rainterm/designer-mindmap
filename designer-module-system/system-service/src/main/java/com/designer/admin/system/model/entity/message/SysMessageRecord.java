package com.designer.admin.system.model.entity.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.DateTimePattern;
import lombok.Data;
import org.noear.snack4.annotation.ONodeAttr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息发送记录
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_message_record")
public class SysMessageRecord implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通道配置 ID */
    private Long configId;

    /** 配置名称快照 */
    private String configName;

    /** 通道类型 */
    private String channelType;

    /** 收件人（逗号分隔） */
    private String receivers;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 是否 HTML：1是 0否 */
    private Integer html;

    /** 状态：1成功 0失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 发送人账号 */
    private String sendBy;

    /** 发送时间 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime sendTime;
}
