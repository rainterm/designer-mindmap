package com.designer.admin.framework.message;

/**
 * 消息通道配置数据源 SPI：由 system 模块实现。
 *
 * @author 雨夜终端
 */
public interface MessageConfigSource {

    /**
     * 获取指定通道的默认运行时配置。
     *
     * @param channelType 通道标识，如 email / inbox
     * @return 运行时配置
     */
    MessageRuntimeConfig requireDefaultRuntime(String channelType);

    /**
     * 按配置 ID 获取运行时配置。
     *
     * @param configId 配置 ID
     * @return 运行时配置
     */
    MessageRuntimeConfig requireRuntimeById(Long configId);
}
