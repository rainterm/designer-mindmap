package com.designer.admin.system.model.entity.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * 站内消息（消息通道 inbox 投递；用户在顶部消息中心接收）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 状态：1正常 0关闭 */
    private Integer status;

    /** 是否全体用户：1是 0否；可见性以本字段 + sys_notice_receiver 为准（内部字段，不返回前端） */
    @ONodeAttr(ignore = true)
    private Integer receiverAll;

    /** 接收人原文（账号或ID，逗号分隔）；仅排查用，不参与查询，也不返回前端（避免泄露其他接收人） */
    @ONodeAttr(ignore = true)
    private String receivers;

    /** 备注 */
    private String remark;

    /** 当前用户是否已读（非库字段） */
    @TableField(exist = false)
    private Boolean readFlag;

    /** 前端跳转路径（非库字段，由 remark 解析） */
    @TableField(exist = false)
    private String link;
}
