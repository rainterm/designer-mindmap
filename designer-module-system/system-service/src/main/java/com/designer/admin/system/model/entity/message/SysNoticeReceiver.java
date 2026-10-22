package com.designer.admin.system.model.entity.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 站内消息接收人（联合主键：notice_id + user_id，无自增主键）。
 *
 * <p>只存定向消息；全体消息由 {@code sys_notice.receiver_all = 1} 表达，
 * 不给每个用户写一行，否则用户数 × 消息数会迅速膨胀。</p>
 *
 * @author 雨夜终端
 */
@Data
@TableName("sys_notice_receiver")
public class SysNoticeReceiver implements Serializable {

    /** 消息ID */
    @TableField("notice_id")
    private Long noticeId;

    /** 接收人用户ID */
    @TableField("user_id")
    private Long userId;
}
