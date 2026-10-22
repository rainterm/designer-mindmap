package com.designer.admin.framework.message;

import com.designer.admin.common.exception.BizException;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 消息发送门面：按通道类型取默认配置（或指定配置）并委托 Provider，同时写入发送记录。
 *
 * @author 雨夜终端
 */
@Component
public class MessageService {

    @Inject
    private MessageChannelRegistry channelRegistry;

    @Inject
    private MessageConfigSource messageConfigSource;

    @Inject
    private MessageRecordWriter messageRecordWriter;

    /** 使用某通道的默认启用配置发送 */
    public void send(String channelType, MessagePayload payload) {
        MessageRuntimeConfig config = messageConfigSource.requireDefaultRuntime(channelType);
        dispatch(config, payload);
    }

    /** 使用指定配置发送 */
    public void sendByConfigId(Long configId, MessagePayload payload) {
        if (configId == null) {
            throw new BizException("消息配置 ID 不能为空");
        }
        MessageRuntimeConfig config = messageConfigSource.requireRuntimeById(configId);
        dispatch(config, payload);
    }

    /** 使用已解析的运行时配置发送（供测试发送等复用） */
    public void dispatch(MessageRuntimeConfig config, MessagePayload payload) {
        if (config == null) {
            throw new BizException("消息配置不能为空");
        }
        long start = System.currentTimeMillis();
        try {
            channelRegistry.getRequired(config.getChannelType()).send(payload, config);
            messageRecordWriter.recordAsync(config, payload, true, null, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            messageRecordWriter.recordAsync(config, payload, false, e.getMessage(), System.currentTimeMillis() - start);
            throw e;
        }
    }
}
