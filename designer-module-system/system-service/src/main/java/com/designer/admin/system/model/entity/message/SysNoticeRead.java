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
 * 通知公告已读记录
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_notice_read")
public class SysNoticeRead implements Serializable {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息ID */
    private Long noticeId;

    /** 阅读人用户ID */
    private Long userId;

    /** 阅读时间 */
    @ONodeAttr(format = DateTimePattern.DATE_TIME)
    private LocalDateTime readTime;
}
