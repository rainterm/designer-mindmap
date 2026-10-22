package com.designer.admin.system.model.vo.file;

import lombok.Data;

/**
 * 上传时可选的存储配置（仅公开项）。
 *
 * <p>只暴露选择器需要的字段：配置里的密钥类参数（S3 AK/SK 等）一律不下发，
 * 所以这里不复用 {@code sys_storage_config} 实体。</p>
 *
 * @author 雨夜终端
 */
@Data
public class StorageOptionVO {

    /** 存储配置ID */
    private Long id;

    /** 配置名称，作为下拉项文案 */
    private String configName;

    /** 存储类型：local / s3 / ... */
    private String storageType;

    /** 是否默认：1是 0否（前端据此默认选中） */
    private Integer isDefault;

    /** 单文件大小上限（字节），前端可据此提前拦截大文件 */
    private Long maxSize;
}
