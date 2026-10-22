package com.designer.admin.system.model.dto.job;

import com.designer.admin.common.core.PageQuery;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务分页查询入参。
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobQueryDTO extends PageQuery {

    @ApiModelProperty("任务名称，模糊匹配")
    private String jobName;

    @ApiModelProperty("任务分组")
    private String jobGroup;

    @ApiModelProperty("状态：1正常 0暂停，为空表示不限")
    private Integer status;
}
