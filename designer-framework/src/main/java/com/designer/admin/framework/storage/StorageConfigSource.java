package com.designer.admin.framework.storage;

/**
 * 存储配置数据源 SPI：由 system 模块实现。
 *
 * @author 雨夜终端
 */
public interface StorageConfigSource {

    /**
     * 获取默认存储的运行时配置。
     *
     * @return 运行时配置
     */
    StorageRuntimeConfig requireDefaultRuntime();

    /**
     * 按配置 ID 获取运行时配置。
     *
     * @param configId 配置 ID
     * @return 运行时配置
     */
    StorageRuntimeConfig requireRuntimeById(Long configId);
}
