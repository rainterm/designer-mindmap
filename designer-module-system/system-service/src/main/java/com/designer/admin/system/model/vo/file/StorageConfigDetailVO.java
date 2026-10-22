package com.designer.admin.system.model.vo.file;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 存储配置详情视图（编辑页回显）。
 *
 * <p><b>为什么 config 仍是 Map</b>：字段集合由存储类型（Provider）在运行时声明，
 * 前端按 {@link StorageTypeVO#getFields()} 渲染动态表单，后端无法在编译期穷举。
 * 其余字段都是固定列，因此显式声明为属性 —— 拼错键名会在编译期报错，而不是在页面上显示空白。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class StorageConfigDetailVO {

    /** 配置 ID */
    private Long id;

    /** 配置名称 */
    private String configName;

    /** 存储类型：local / s3 / ... */
    private String storageType;

    /** 是否默认：1是 0否 */
    private Integer isDefault;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 是否公开访问：1公开 0私有 */
    private Integer isPublic;

    /** 下载地址前缀（由访问类型决定） */
    private String urlPrefix;

    /** 允许上传的扩展名，逗号分隔 */
    private String allowedExtensions;

    /** 单文件大小上限（字节） */
    private Long maxSize;

    /** 上传图片是否压缩：1是 0否 */
    private Integer compressEnabled;

    /** JPEG 压缩质量：10-100，越小体积越小 */
    private Integer compressQuality;

    /** 备注 */
    private String remark;

    /** 类型专属配置项（密钥字段已脱敏），键集合由存储类型决定 */
    private Map<String, Object> config;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
