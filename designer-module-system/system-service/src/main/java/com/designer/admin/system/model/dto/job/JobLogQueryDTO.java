package com.designer.admin.system.model.dto.job;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调度日志分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobLogQueryDTO extends PageQuery {

    @ApiModelProperty("任务 ID，为空表示不限")
    private Long jobId;

    @ApiModelProperty("任务名称，模糊匹配")
    private String jobName;

    @ApiModelProperty("任务分组")
    private String jobGroup;

    @ApiModelProperty("执行状态：1成功 0失败，为空表示不限")
    private Integer status;
}
