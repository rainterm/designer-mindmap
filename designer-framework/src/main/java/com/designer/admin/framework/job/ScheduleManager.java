package com.designer.admin.framework.job;

import com.designer.admin.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

/**
 * Quartz 调度器封装：创建 / 更新 / 删除 / 暂停 / 恢复 / 立即执行
 *
 * <p><b>存储基于 DB</b>：Quartz 用 JDBC JobStore（配置见 app.yml 的 {@code org.quartz.*}），
 * 任务与触发器状态落在 {@code QRTZ_*} 表，多实例通过数据库行锁协调，
 * 同一个任务同一时刻只会在一个节点执行。</p>
 *
 * <p><b>为什么不放 quartz.properties</b>：只想维护一份配置文件。Quartz 的
 * {@code getDefaultScheduler()} 只会去 classpath 找固定文件，这里改为从 Solon 配置
 * 收集 {@code org.quartz.*} 前缀的键，构造 {@link StdSchedulerFactory}，
 * 顺带获得环境变量覆盖与配置中心能力。</p>
 *
 * <p><b>为什么启动同步要单独一条路径</b>：{@link #createJob} 若沿用「存在就先删再建」，
 * 每个实例启动都会删掉别的实例正在用的触发器 —— 下次触发时间被重置、认领中的任务丢失，
 * 多节点滚动重启时还会互相打断。因此启动时走 {@link #syncJob}：只补差异，不动已经正确的调度。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class ScheduleManager {

    /** Quartz 原生属性的前缀；配置原样透传，不做键名映射 */
    private static final String QUARTZ_PREFIX = "org.quartz.";

    private Scheduler scheduler;

    @Init(index = 1)
    public void start() throws SchedulerException {
        scheduler = new StdSchedulerFactory(loadQuartzProps()).getScheduler();
        scheduler.start();
        log.info("Quartz Scheduler started（JDBC JobStore，集群模式）");
    }

    /**
     * 从 Solon 配置收集 Quartz 属性。
     *
     * <p>键名保持 {@code org.quartz.*} 原样，因此 app.yml 里的写法与 Quartz 官方文档一一对应，
     * 不需要在代码里维护一张「内部键名 → Quartz 键名」的映射表（那种映射表最容易随版本漂移）。</p>
     *
     * @return Quartz 配置
     */
    private Properties loadQuartzProps() {
        Properties props = new Properties();
        Solon.cfg().forEach((k, v) -> {
            if (k instanceof String key && v != null && key.startsWith(QUARTZ_PREFIX)) {
                props.setProperty(key, String.valueOf(v));
            }
        });
        if (props.isEmpty()) {
            // 静默退化为 RAMJobStore 是最坏的结果：单机看起来一切正常，
            // 多实例上线后才暴露重复执行。所以宁可直接启动失败。
            throw new IllegalStateException("未找到 Quartz 配置：请在 app.yml 中配置 org.quartz.* "
                    + "（调度器存储已从 quartz.properties 迁移到 app.yml）");
        }
        return props;
    }

    @Destroy
    public void stop() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
                log.info("Quartz Scheduler stopped");
            } catch (SchedulerException e) {
                log.warn("Quartz Scheduler shutdown error", e);
            }
        }
    }

    /**
     * 新建任务；已存在则按更新处理（保持幂等，重复调用不会破坏已有触发器）。
     *
     * @param job 任务定义
     */
    public void createJob(JobModel job) {
        try {
            JobKey jobKey = jobKey(job);
            if (scheduler.checkExists(jobKey)) {
                updateJob(job);
                return;
            }
            scheduler.scheduleJob(newJobDetail(job), newCronTrigger(job));
            alignStatus(job);
        } catch (ObjectAlreadyExistsException e) {
            // 多节点同时启动（如副本数从 0 扩到 N）时，另一个节点可能刚创建了同一任务。
            // 这不是错误：退化为对账即可，否则启动日志会出现一条吓人的失败记录。
            syncJob(job);
        } catch (SchedulerException e) {
            throw new BizException("创建定时任务失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务：覆盖 JobDetail（含 JobDataMap）并重建触发器，保留任务键。
     *
     * @param job 任务定义
     */
    public void updateJob(JobModel job) {
        try {
            JobKey jobKey = jobKey(job);
            if (!scheduler.checkExists(jobKey)) {
                createJob(job);
                return;
            }
            // 覆盖 JobDetail：下次执行即可读到新的 invokeTarget / 并发设置
            scheduler.addJob(newJobDetail(job), true);
            // 重建触发器而不是先删任务：deleteJob 会连带删掉 JobDetail，
            // 集群里其它节点可能刚认领到它
            TriggerKey triggerKey = triggerKey(job);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.rescheduleJob(triggerKey, newCronTrigger(job));
            } else {
                try {
                    scheduler.scheduleJob(newCronTrigger(job));
                } catch (ObjectAlreadyExistsException ignored) {
                    // 另一个节点刚补上触发器，说明目标状态已经达成
                    log.debug("触发器已由其它节点创建 name={} group={}", triggerKey.getName(), triggerKey.getGroup());
                }
            }
            alignStatus(job);
        } catch (SchedulerException e) {
            throw new BizException("更新定时任务失败：" + e.getMessage());
        }
    }

    /**
     * 启动时对账：以 {@code sys_job} 为准补齐 Quartz 侧缺失或与实际配置不一致的部分。
     *
     * <p>与 {@link #updateJob} 的区别是「能不动就不动」：cron 与 JobDataMap 都一致时
     * 不碰触发器，避免每次重启都把下次触发时间重置（那会让固定时刻执行的任务
     * 在频繁发版期间永远等不到触发）。</p>
     *
     * @param job 任务定义
     */
    public void syncJob(JobModel job) {
        try {
            JobKey jobKey = jobKey(job);
            if (!scheduler.checkExists(jobKey)) {
                createJob(job);
                return;
            }

            JobDetail detail = scheduler.getJobDetail(jobKey);
            Object scheduled = detail == null ? null : detail.getJobDataMap().get(ScheduleConsts.TASK_PROPERTIES);
            if (!job.equals(scheduled)) {
                // JobModel 是 @Data，equals 逐字段比较；不一致说明任务定义在库侧改过
                scheduler.addJob(newJobDetail(job), true);
            }

            TriggerKey triggerKey = triggerKey(job);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                // 与 createJob 相同的并发情形：另一个节点可能刚补上触发器
                try {
                    scheduler.scheduleJob(newCronTrigger(job));
                } catch (ObjectAlreadyExistsException ignored) {
                    log.debug("触发器已由其它节点创建 name={} group={}", triggerKey.getName(), triggerKey.getGroup());
                }
            } else if (!(trigger instanceof CronTrigger cron) || !job.getCronExpression().equals(cron.getCronExpression())) {
                scheduler.rescheduleJob(triggerKey, newCronTrigger(job));
            }
            alignStatus(job);
        } catch (SchedulerException e) {
            throw new BizException("同步定时任务失败：" + e.getMessage());
        }
    }

    public void deleteJob(JobModel job) {
        try {
            scheduler.deleteJob(jobKey(job));
        } catch (SchedulerException e) {
            throw new BizException("删除定时任务失败：" + e.getMessage());
        }
    }

    public void pauseJob(JobModel job) {
        try {
            scheduler.pauseJob(jobKey(job));
        } catch (SchedulerException e) {
            throw new BizException("暂停定时任务失败：" + e.getMessage());
        }
    }

    public void resumeJob(JobModel job) {
        try {
            scheduler.resumeJob(jobKey(job));
        } catch (SchedulerException e) {
            throw new BizException("恢复定时任务失败：" + e.getMessage());
        }
    }

    public void runJobOnce(JobModel job) {
        try {
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(ScheduleConsts.TASK_PROPERTIES, job);
            JobKey key = jobKey(job);
            if (!scheduler.checkExists(key)) {
                createJob(job);
            }
            scheduler.triggerJob(key, dataMap);
        } catch (SchedulerException e) {
            throw new BizException("立即执行失败：" + e.getMessage());
        }
    }

    /**
     * 让 Quartz 侧的启停状态与 {@code sys_job.status} 对齐。
     *
     * <p>先读状态再决定动作，而不是无脑 pause / resume：
     * 后者在集群里会周期性改写 {@code QRTZ_TRIGGERS}，与其它节点的认领产生不必要的锁竞争。</p>
     */
    private void alignStatus(JobModel job) throws SchedulerException {
        TriggerKey triggerKey = triggerKey(job);
        if (!scheduler.checkExists(triggerKey)) {
            return;
        }
        boolean paused = scheduler.getTriggerState(triggerKey) == Trigger.TriggerState.PAUSED;
        boolean wantPause = job.getStatus() != null && job.getStatus() == ScheduleConsts.STATUS_PAUSE;
        if (wantPause && !paused) {
            scheduler.pauseJob(jobKey(job));
        } else if (!wantPause && paused) {
            scheduler.resumeJob(jobKey(job));
        }
    }

    private JobDetail newJobDetail(JobModel job) {
        JobDetail jobDetail = JobBuilder.newJob(resolveJobClass(job))
                .withIdentity(jobKey(job))
                // JDBC JobStore 下 addJob(replace=true) 要求 JobDetail 是 durable 的：
                // 非 durable 任务一旦没有关联触发器就会被拒绝（"Jobs added with no trigger must be durable"）。
                // 任务的删除由 deleteJob 显式完成，这里的 durable 只表示「不要因为暂时没有触发器就清理我」。
                .storeDurably()
                .build();
        jobDetail.getJobDataMap().put(ScheduleConsts.TASK_PROPERTIES, job);
        return jobDetail;
    }

    private CronTrigger newCronTrigger(JobModel job) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        scheduleBuilder = handleMisfire(scheduleBuilder, job.getMisfirePolicy());
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(job))
                .withSchedule(scheduleBuilder)
                .build();
    }

    private CronScheduleBuilder handleMisfire(CronScheduleBuilder builder, Integer policy) {
        if (policy == null) {
            return builder;
        }
        return switch (policy) {
            case ScheduleConsts.MISFIRE_IGNORE_MISFIRES -> builder.withMisfireHandlingInstructionIgnoreMisfires();
            case ScheduleConsts.MISFIRE_DO_NOTHING -> builder.withMisfireHandlingInstructionDoNothing();
            default -> builder.withMisfireHandlingInstructionFireAndProceed();
        };
    }

    private Class<? extends org.quartz.Job> resolveJobClass(JobModel job) {
        if (job.getConcurrent() != null && job.getConcurrent() == ScheduleConsts.CONCURRENT_DISALLOW) {
            return QuartzDisallowConcurrentExecution.class;
        }
        return QuartzJobExecution.class;
    }

    public static JobKey jobKey(JobModel job) {
        return JobKey.jobKey(ScheduleConsts.TASK_CLASS_NAME + job.getId(), job.getJobGroup());
    }

    public static TriggerKey triggerKey(JobModel job) {
        return TriggerKey.triggerKey(ScheduleConsts.TASK_CLASS_NAME + job.getId(), job.getJobGroup());
    }
}
