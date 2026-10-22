package com.designer.admin.system.model.entity.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息通道配置（数据库驱动，可扩展 email / inbox / sms / dingtalk / ...）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_config")
public class SysMessageConfig extends BaseEntity {

    /** 配置名称 */
    private String configName;

    /** 通道类型：email / inbox / sms / dingtalk / wechat / feishu */
    private String channelType;

    /** 同通道类型下是否默认 */
    private Integer isDefault;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 通道参数（JSON，字段由各通道的 {@code ConfigFieldMeta} 描述） */
    private String configJson;

    /** 备注 */
    private String remark;
}
