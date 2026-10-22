package com.designer.admin.framework.storage;

import lombok.Builder;
import lombok.Data;

/**
 * 文件落盘/上传成功后的元信息
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class StoredFile {

    /** 原始文件名 */
    private String originalName;

    /** 存储文件名（UUID） */
    private String fileName;

    /** 存储相对路径 / 对象 Key */
    private String filePath;

    /** 对外访问 URL */
    private String url;

    /** 文件大小（字节） */
    private long fileSize;

    /** Content-Type */
    private String contentType;

    /** 扩展名（小写，无点） */
    private String extension;

    /** 存储类型：local / s3 */
    private String storageType;

    /** 配置唯一标识 */
    private Long configId;
}
