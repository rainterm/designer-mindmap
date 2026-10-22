package com.designer.admin.system.model.dto.file;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件分页查询条件
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileQueryDTO extends PageQuery {

    @ApiModelProperty("原始文件名，模糊匹配")
    private String originalName;

    @ApiModelProperty("业务子目录")
    private String bizDir;

    @ApiModelProperty("存储类型：local / s3")
    private String storageType;

    @ApiModelProperty("扩展名")
    private String extension;

    @ApiModelProperty("创建人")
    private String createBy;

    @ApiModelProperty("创建时间起，格式 yyyy-MM-dd")
    private String beginTime;

    @ApiModelProperty("创建时间止，格式 yyyy-MM-dd")
    private String endTime;
}
