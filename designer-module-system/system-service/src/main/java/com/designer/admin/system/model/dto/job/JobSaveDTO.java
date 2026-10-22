package com.designer.admin.system.model.dto.job;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 定时任务新增 / 修改入参。
 *
 * @author 雨夜终端
 */
@Data
public class JobSaveDTO {

    @ApiModelProperty("任务 ID，新增时留空")
    private Long id;

    @ApiModelProperty("任务名称")
    private String jobName;

    @ApiModelProperty("任务分组")
    private String jobGroup;

    @ApiModelProperty("调用目标，格式为 beanName.methodName(参数)")
    private String invokeTarget;

    @ApiModelProperty("Cron 表达式")
    private String cronExpression;

    @ApiModelProperty("misfire 策略：1默认 2立即执行 3放弃执行")
    private Integer misfirePolicy;

    @ApiModelProperty("是否允许并发：0否 1是")
    private Integer concurrent;

    @ApiModelProperty("状态：1正常 0暂停")
    private Integer status;

    @ApiModelProperty("备注")
    private String remark;
}
