package com.designer.admin.framework.job;

import org.noear.solon.core.util.RunUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;

/**
 * Quartz 抽象任务：执行 invoke_target 并写调度日志
 *
 * @author 雨夜终端
 */
@Slf4j
public abstract class AbstractQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        JobModel job = (JobModel) context.getMergedJobDataMap().get(ScheduleConsts.TASK_PROPERTIES);
        long start = System.currentTimeMillis();
        try {
            before(context, job);
            if (job != null) {
                doExecute(context, job);
            }
            after(context, job, null, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("任务执行异常 jobId={}", job == null ? null : job.getId(), e);
            after(context, job, e, System.currentTimeMillis() - start);
        }
    }

    /**
     * 执行前钩子，默认空实现。
     *
     * @param context Quartz 执行上下文
     * @param job     任务定义
     */
    protected void before(JobExecutionContext context, JobModel job) {
        // hook
    }

    /**
     * 执行任务主体。
     *
     * @param context Quartz 执行上下文
     * @param job     任务定义
     * @throws Exception 任务执行失败时抛出
     */
    protected abstract void doExecute(JobExecutionContext context, JobModel job) throws Exception;

    /**
     * 执行后钩子：记录调度日志。
     *
     * @param context Quartz 执行上下文
     * @param job     任务定义
     * @param e       执行异常，成功时为 {@code null}
     * @param cost    执行耗时（毫秒）
     */
    protected void after(JobExecutionContext context, JobModel job, Exception e, long cost) {
        if (job == null) {
            return;
        }
        JobLogRecord jobLog = new JobLogRecord();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setCostTime(cost);
        jobLog.setCreateTime(LocalDateTime.now());
        if (e != null) {
            jobLog.setStatus(0);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            jobLog.setJobMessage(job.getJobName() + " 执行失败，耗时 " + cost + " ms");
            jobLog.setExceptionInfo(msg.length() > 2000 ? msg.substring(0, 2000) : msg);
        } else {
            jobLog.setStatus(1);
            jobLog.setJobMessage(job.getJobName() + " 执行成功，耗时 " + cost + " ms");
            jobLog.setExceptionInfo("");
        }
        RunUtil.asyncAndTry(() -> {
            JobLogWriter writer = Solon.context().getBean(JobLogWriter.class);
            if (writer != null) {
                writer.write(jobLog);
            }
        });
    }
}
