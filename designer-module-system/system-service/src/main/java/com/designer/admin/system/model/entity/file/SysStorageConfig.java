package com.designer.admin.system.model.entity.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件存储配置（数据库驱动，可扩展多种存储类型）
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_storage_config")
public class SysStorageConfig extends BaseEntity {

    /** 配置名称 */
    private String configName;

    /** 存储类型：local / s3 / ...（对应 FileStorageProvider#type） */
    private String storageType;

    /** 是否默认：1是 0否（全局仅一条为默认） */
    private Integer isDefault;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 是否公开访问：1公开（/app-api 匿名可访问） 0私有（/admin-api 需登录） */
    private Integer isPublic;

    /** 代理下载 URL 前缀（按 isPublic 自动生成） */
    private String urlPrefix;

    /** 允许的扩展名，逗号分隔 */
    private String allowedExtensions;

    /** 单文件大小上限（字节） */
    private Long maxSize;

    /** 上传图片是否压缩：1是 0否 */
    private Integer compressEnabled;

    /** JPEG 压缩质量：10-100，越小体积越小；PNG 为无损压缩、忽略该值 */
    private Integer compressQuality;

    /** 类型专属配置 JSON */
    private String configJson;

    /** 备注 */
    private String remark;
}
