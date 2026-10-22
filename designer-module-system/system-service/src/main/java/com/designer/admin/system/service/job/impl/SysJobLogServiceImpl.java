package com.designer.admin.system.service.job.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import org.noear.solon.core.util.RunUtil;
import com.designer.admin.framework.job.JobLogRecord;
import com.designer.admin.framework.job.JobLogWriter;
import com.designer.admin.system.mapper.job.SysJobLogMapper;
import com.designer.admin.system.model.dto.job.JobLogQueryDTO;
import com.designer.admin.system.model.entity.job.SysJobLog;
import com.designer.admin.system.service.job.SysJobLogService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 定时任务调度日志，同时实现 framework {@link JobLogWriter} SPI。
 *
 * @author 雨夜终端
 */
@Component
public class SysJobLogServiceImpl implements SysJobLogService, JobLogWriter {

    @Db
    private SysJobLogMapper jobLogMapper;

    @Override
    public void write(JobLogRecord record) {
        if (record == null) {
            return;
        }
        SysJobLog log = new SysJobLog();
        log.setJobId(record.getJobId());
        log.setJobName(record.getJobName());
        log.setJobGroup(record.getJobGroup());
        log.setInvokeTarget(record.getInvokeTarget());
        log.setJobMessage(record.getJobMessage());
        log.setStatus(record.getStatus());
        log.setExceptionInfo(record.getExceptionInfo());
        log.setCostTime(record.getCostTime());
        log.setCreateTime(record.getCreateTime());
        addJobLog(log);
    }

    @Override
    public void addJobLog(SysJobLog log) {
        jobLogMapper.insert(log);
    }

    @Override
    public void addJobLogAsync(SysJobLog log) {
        RunUtil.asyncAndTry(() -> jobLogMapper.insert(log));
    }

    @Override
    public IPage<SysJobLog> listPage(JobLogQueryDTO query) {
        return jobLogMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysJobLog>()
                .eq(query.getJobId() != null, SysJobLog::getJobId, query.getJobId())
                .like(notBlank(query.getJobName()), SysJobLog::getJobName, query.getJobName())
                .eq(notBlank(query.getJobGroup()), SysJobLog::getJobGroup, query.getJobGroup())
                .eq(query.getStatus() != null, SysJobLog::getStatus, query.getStatus())
                .orderByDesc(SysJobLog::getId));
    }

    @Override
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的日志");
        }
        jobLogMapper.deleteByIds(ids);
    }

    @Override
    public void removeAll() {
        jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>().ge(SysJobLog::getId, 0));
    }

    @Override
    public int removeOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>()
                .lt(SysJobLog::getCreateTime, before));
    }

    @Override
    public void removeByJobId(Long jobId) {
        if (jobId == null) {
            throw new BizException("任务ID不能为空");
        }
        jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>().eq(SysJobLog::getJobId, jobId));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
