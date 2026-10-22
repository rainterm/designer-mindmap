package com.designer.admin.system.model.dto.log;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQueryDTO extends PageQuery {

    @ApiModelProperty("操作模块，模糊匹配")
    private String title;

    @ApiModelProperty("操作人，模糊匹配")
    private String operName;

    @ApiModelProperty("状态：1成功 0失败，为空表示不限")
    private Integer status;
}
