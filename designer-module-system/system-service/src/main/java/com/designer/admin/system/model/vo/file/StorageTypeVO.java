package com.designer.admin.system.model.vo.file;

import com.designer.admin.system.model.vo.config.ConfigFieldVO;
import lombok.Data;

import java.util.List;

/**
 * 可注册的存储类型（供前端动态表单）
 *
 * @author 雨夜终端
 */
@Data
public class StorageTypeVO {

    /** 存储类型标识：local / s3 / ... */
    private String type;

    /** 类型显示名称 */
    private String label;

    /** 该类型需要填写的配置字段（前端据此渲染动态表单） */
    private List<ConfigFieldVO> fields;

    /** 私有代理下载前缀（来自 app.yml） */
    private String defaultUrlPrefix;

    /** 公开代理下载前缀（来自 app.yml） */
    private String publicUrlPrefix;
}
