package com.designer.admin.framework.message;

/**
 * 消息发送记录写入 SPI：由 system 模块实现。
 *
 * @author 雨夜终端
 */
public interface MessageRecordWriter {

    /**
     * 异步写入一条消息发送记录。
     *
     * @param config   通道运行时配置
     * @param payload  消息内容
     * @param success  是否发送成功
     * @param errorMsg 失败原因，成功时为空
     * @param costMs   发送耗时（毫秒）
     */
    void recordAsync(MessageRuntimeConfig config, MessagePayload payload,
                     boolean success, String errorMsg, long costMs);
}
