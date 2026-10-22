package com.designer.admin.system.model.vo.message;

import com.designer.admin.system.model.vo.config.ConfigFieldVO;
import lombok.Data;

import java.util.List;

/**
 * 消息通道类型元数据出参。
 *
 * @author 雨夜终端
 */
@Data
public class MessageChannelTypeVO {

    /** 通道类型标识：email / inbox / sms / dingtalk / wechat / feishu */
    private String type;

    /** 类型显示名称 */
    private String label;

    /** 该通道需要填写的配置字段（前端据此渲染动态表单） */
    private List<ConfigFieldVO> fields;
}
