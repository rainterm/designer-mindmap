package com.designer.admin.system.service.job.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.job.CronUtils;
import com.designer.admin.framework.job.JobInvokeUtils;
import com.designer.admin.framework.job.JobModel;
import com.designer.admin.framework.job.ScheduleConsts;
import com.designer.admin.framework.job.ScheduleManager;
import com.designer.admin.system.mapper.job.SysJobMapper;
import com.designer.admin.system.model.dto.job.JobQueryDTO;
import com.designer.admin.system.model.dto.job.JobSaveDTO;
import com.designer.admin.system.model.entity.job.SysJob;
import com.designer.admin.system.model.vo.job.JobDetailVO;
import com.designer.admin.system.service.job.SysJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 定时任务：CRUD + 启停/立即执行，并与 Quartz 同步
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class SysJobServiceImpl implements SysJobService {

    @Db
    private SysJobMapper jobMapper;

    @Inject
    private ScheduleManager scheduleManager;

    @Override
    @Init(index = 20)
    public void loadJobs() {
        List<SysJob> jobs = jobMapper.selectList(new LambdaQueryWrapper<SysJob>().orderByAsc(SysJob::getId));
        int failed = 0;
        for (SysJob job : jobs) {
            try {
                // 用 sync 而不是 create：QRTZ_* 里的触发器是持久化的，启动时只需补齐差异。
                // 若这里 delete + create，每次重启都会重置下次触发时间，多节点滚动重启时
                // 还会互相删掉对方正在使用的触发器。
                scheduleManager.syncJob(toJobModel(job));
            } catch (Exception e) {
                failed++;
                log.error("加载定时任务失败 id={} name={}", job.getId(), job.getJobName(), e);
            }
        }
        log.info("已同步定时任务 {} 条（失败 {} 条）", jobs.size(), failed);
    }

    @Override
    public IPage<SysJob> listPage(JobQueryDTO query) {
        return jobMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysJob>()
                .like(notBlank(query.getJobName()), SysJob::getJobName, query.getJobName())
                .eq(notBlank(query.getJobGroup()), SysJob::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJob::getStatus, query.getStatus())
                .orderByDesc(SysJob::getId));
    }

    @Override
    public SysJob getById(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException("任务不存在");
        }
        return job;
    }

    @Override
    public JobDetailVO getDetailView(Long id) {
        SysJob job = getById(id);
        Date next = CronUtils.nextTriggerTime(job.getCronExpression());
        return JobDetailVO.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .jobGroup(job.getJobGroup())
                .invokeTarget(job.getInvokeTarget())
                .cronExpression(job.getCronExpression())
                .misfirePolicy(job.getMisfirePolicy())
                .concurrent(job.getConcurrent())
                .status(job.getStatus())
                .remark(job.getRemark())
                .createTime(job.getCreateTime())
                .updateTime(job.getUpdateTime())
                .nextValidTime(next == null ? null
                        : next.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }

    @Override
    @Transaction
    public void add(JobSaveDTO dto) {
        SysJob job = toEntity(dto, true);
        validateJob(job);
        jobMapper.insert(job);
        scheduleManager.createJob(toJobModel(job));
    }

    @Override
    @Transaction
    public void update(JobSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("任务ID不能为空");
        }
        SysJob exist = getById(dto.getId());
        SysJob job = toEntity(dto, false);
        job.setId(exist.getId());
        validateJob(job);
        jobMapper.updateById(job);
        scheduleManager.updateJob(toJobModel(job));
    }

    @Override
    @Transaction
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的任务");
        }
        for (Long id : ids) {
            SysJob job = jobMapper.selectById(id);
            if (job != null) {
                scheduleManager.deleteJob(toJobModel(job));
                jobMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transaction
    public void updateStatus(Long id, Integer status) {
        if (!isValidStatus(status)) {
            throw new BizException("状态无效");
        }
        SysJob job = getById(id);
        job.setStatus(status);
        jobMapper.updateById(job);
        if (status == ScheduleConsts.STATUS_NORMAL) {
            scheduleManager.resumeJob(toJobModel(job));
        } else {
            scheduleManager.pauseJob(toJobModel(job));
        }
    }

    @Override
    public void runOnce(Long id) {
        SysJob job = getById(id);
        scheduleManager.runJobOnce(toJobModel(job));
    }

    private JobModel toJobModel(SysJob job) {
        JobModel model = new JobModel();
        model.setId(job.getId());
        model.setJobName(job.getJobName());
        model.setJobGroup(job.getJobGroup());
        model.setInvokeTarget(job.getInvokeTarget());
        model.setCronExpression(job.getCronExpression());
        model.setMisfirePolicy(job.getMisfirePolicy());
        model.setConcurrent(job.getConcurrent());
        model.setStatus(job.getStatus());
        return model;
    }

    private void validateJob(SysJob job) {
        if (!notBlank(job.getJobName())) {
            throw new BizException("任务名称不能为空");
        }
        if (!notBlank(job.getInvokeTarget())) {
            throw new BizException("调用目标不能为空");
        }
        if (!notBlank(job.getCronExpression()) || !CronUtils.isValid(job.getCronExpression())) {
            throw new BizException("Cron 表达式不正确");
        }
        JobInvokeUtils.validate(job.getInvokeTarget());
    }

    private SysJob toEntity(JobSaveDTO dto, boolean creating) {
        SysJob job = new SysJob();
        job.setId(dto.getId());
        job.setJobName(trim(dto.getJobName()));
        job.setJobGroup(notBlank(dto.getJobGroup()) ? dto.getJobGroup().trim() : "DEFAULT");
        job.setInvokeTarget(trim(dto.getInvokeTarget()));
        job.setCronExpression(trim(dto.getCronExpression()));
        job.setMisfirePolicy(dto.getMisfirePolicy() == null ? ScheduleConsts.MISFIRE_DEFAULT : dto.getMisfirePolicy());
        job.setConcurrent(dto.getConcurrent() == null ? ScheduleConsts.CONCURRENT_ALLOW : dto.getConcurrent());
        job.setStatus(dto.getStatus() == null
                ? (creating ? ScheduleConsts.STATUS_PAUSE : ScheduleConsts.STATUS_NORMAL)
                : dto.getStatus());
        job.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return job;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 判断状态是否为合法值（正常 / 暂停）。
     *
     * @param status 状态值
     * @return 合法时返回 {@code true}
     */
    private boolean isValidStatus(Integer status) {
        if (status == null) {
            return false;
        }
        return status == ScheduleConsts.STATUS_NORMAL || status == ScheduleConsts.STATUS_PAUSE;
    }
}
