package com.designer.admin.system.model.vo.job;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务详情视图（编辑页回显 + 下次触发时间）。
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class JobDetailVO {

    /** 任务 ID */
    private Long id;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** 调用目标：beanName.method(args) */
    private String invokeTarget;

    /** cron 表达式 */
    private String cronExpression;

    /** 错过触发策略 */
    private Integer misfirePolicy;

    /** 是否允许并发：1允许 0禁止 */
    private Integer concurrent;

    /** 状态：1正常 0暂停 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 下次触发时间；cron 非法时为 null */
    private LocalDateTime nextValidTime;
}
