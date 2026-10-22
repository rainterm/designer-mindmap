package com.designer.admin.framework.storage;

import com.designer.admin.framework.spi.ConfigFieldMeta;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 文件存储 SPI：新增存储方式时实现本接口并注册为 Solon 组件即可。
 *
 * @author 雨夜终端
 */
public interface FileStorageProvider {

    /**
     * 类型标识，写入库表 {@code storage_type}。
     *
     * @return 存储类型，如 local / s3
     */
    String type();

    /**
     * 前端展示名称。
     *
     * @return 存储方式名称
     */
    String label();

    /**
     * 类型专属配置字段（动态表单）。
     *
     * @return 配置字段元数据列表
     */
    List<ConfigFieldMeta> configFields();

    /**
     * 校验类型专属配置。
     *
     * @param props 配置项
     */
    void validateConfig(Map<String, Object> props);

    /**
     * 上传文件。
     *
     * <p>入参是数据流而不是 {@code UploadedFile}：门面可能在落盘前先做图片压缩，
     * 此时要写的是重编码后的字节，而不是原始上传流。</p>
     *
     * @param in          文件内容，<b>由调用方负责关闭</b>
     * @param size        内容长度（压缩后）
     * @param filePath    目标相对路径
     * @param contentType 内容类型
     * @param config      运行时配置
     */
    void store(InputStream in, long size, String filePath, String contentType, StorageRuntimeConfig config);

    /**
     * 删除物理文件（忽略不存在）。
     *
     * @param filePath 相对路径
     * @param config   运行时配置
     */
    void delete(String filePath, StorageRuntimeConfig config);

    /**
     * 打开文件流，调用方负责关闭。
     *
     * @param filePath 相对路径
     * @param config   运行时配置
     * @return 文件输入流
     * @throws Exception 读取失败时抛出
     */
    InputStream open(String filePath, StorageRuntimeConfig config) throws Exception;

    /**
     * 生成对外访问 URL。
     *
     * @param filePath 相对路径
     * @param config   运行时配置
     * @return 可直接访问的 URL
     */
    String buildAccessUrl(String filePath, StorageRuntimeConfig config);
}
