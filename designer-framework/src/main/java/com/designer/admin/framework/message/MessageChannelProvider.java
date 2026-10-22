package com.designer.admin.framework.message;

import com.designer.admin.framework.spi.ConfigFieldMeta;

import java.util.List;
import java.util.Map;

/**
 * 消息通道 SPI：新增短信 / 钉钉 / 微信 / 飞书时实现本接口并注册为组件即可。
 *
 * @author 雨夜终端
 */
public interface MessageChannelProvider {

    /**
     * 通道标识。
     *
     * @return 通道标识：email / inbox / sms / dingtalk / wechat / feishu
     */
    String type();

    /**
     * 前端展示名称。
     *
     * @return 通道名称
     */
    String label();

    /**
     * 类型专属配置字段。
     *
     * @return 配置字段元数据列表
     */
    List<ConfigFieldMeta> configFields();

    /**
     * 校验通道配置。
     *
     * @param props 配置项
     */
    void validateConfig(Map<String, Object> props);

    /**
     * 发送消息。
     *
     * @param payload 消息内容
     * @param config  通道运行时配置
     */
    void send(MessagePayload payload, MessageRuntimeConfig config);
}
