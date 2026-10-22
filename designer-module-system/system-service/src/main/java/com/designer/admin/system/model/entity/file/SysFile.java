package com.designer.admin.system.model.entity.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统文件元数据
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    /** 原始文件名 */
    private String originalName;

    /** 存储文件名 */
    private String fileName;

    /** 存储相对路径 / 对象 Key */
    private String filePath;

    /** 访问 URL */
    private String url;

    /** 文件大小（字节） */
    private Long fileSize;

    /** Content-Type */
    private String contentType;

    /** 扩展名 */
    private String extension;

    /** 业务子目录 */
    private String bizDir;

    /** 存储类型：local / s3 */
    private String storageType;

    /** 使用的存储配置 ID */
    private Long configId;
}
