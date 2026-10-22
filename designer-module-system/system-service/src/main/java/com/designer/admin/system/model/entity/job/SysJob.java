package com.designer.admin.system.model.entity.job;

import com.baomidou.mybatisplus.annotation.TableName;
import com.designer.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务
 *
 * @author 雨夜终端
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity {

    /** 任务名称 */
    private String jobName;

    /** 任务组名 */
    private String jobGroup;

    /** 调用目标：bean.method / bean.method(args) */
    private String invokeTarget;

    /** cron 表达式 */
    private String cronExpression;

    /** misfire 策略：1默认 2立即执行 3放弃 */
    private Integer misfirePolicy;

    /** 是否允许并发：1允许 0禁止 */
    private Integer concurrent;

    /** 状态：1正常 0暂停 */
    private Integer status;

    /** 备注 */
    private String remark;
}
