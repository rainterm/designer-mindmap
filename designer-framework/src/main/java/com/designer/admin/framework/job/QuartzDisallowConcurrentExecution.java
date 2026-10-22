package com.designer.admin.framework.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 禁止并发执行的 Quartz Job
 *
 * @author 雨夜终端
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    protected void doExecute(JobExecutionContext context, JobModel job) throws Exception {
        JobInvokeUtils.invokeMethod(job.getInvokeTarget());
    }
}
