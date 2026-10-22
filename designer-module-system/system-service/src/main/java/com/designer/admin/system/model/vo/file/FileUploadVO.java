package com.designer.admin.system.model.vo.file;

import com.designer.admin.system.model.entity.file.SysFile;
import lombok.Data;

/**
 * 文件上传结果。
 *
 * <p>只回传前端上传组件真正用得到的字段：{@code filePath}、{@code contentType}、
 * {@code extension} 等存储细节属于服务端内部信息，不下发。</p>
 *
 * @author 雨夜终端
 */
@Data
public class FileUploadVO {

    /** 文件 ID（sys_file 主键） */
    private Long id;

    /** 访问 URL */
    private String url;

    /** 原始文件名，供前端展示 */
    private String name;

    /** 存储文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 存储类型：local / s3 */
    private String storageType;

    /** 实际使用的存储配置 ID */
    private Long configId;

    /**
     * 由文件元数据转换出上传结果。
     *
     * @param file 已落库的文件元数据
     * @return 上传结果；入参为 {@code null} 时返回 {@code null}
     */
    public static FileUploadVO from(SysFile file) {
        if (file == null) {
            return null;
        }
        FileUploadVO vo = new FileUploadVO();
        vo.setId(file.getId());
        vo.setUrl(file.getUrl());
        vo.setName(file.getOriginalName());
        vo.setFileName(file.getFileName());
        vo.setFileSize(file.getFileSize());
        vo.setStorageType(file.getStorageType());
        vo.setConfigId(file.getConfigId());
        return vo;
    }
}
