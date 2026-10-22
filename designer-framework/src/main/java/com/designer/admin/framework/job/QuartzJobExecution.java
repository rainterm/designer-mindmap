package com.designer.admin.framework.job;

import org.quartz.JobExecutionContext;

/**
 * 允许并发执行的 Quartz Job
 *
 * @author 雨夜终端
 */
public class QuartzJobExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, JobModel job) throws Exception {
        JobInvokeUtils.invokeMethod(job.getInvokeTarget());
    }
}
