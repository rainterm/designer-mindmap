package com.designer.admin.system.service.job;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.framework.job.JobLogRecord;
import com.designer.admin.framework.job.JobLogWriter;
import com.designer.admin.system.model.dto.job.JobLogQueryDTO;
import com.designer.admin.system.model.entity.job.SysJobLog;

import java.util.List;

/**
 * 定时任务调度日志，同时实现 framework {@link JobLogWriter} SPI。
 *
 * @author 雨夜终端
 */
public interface SysJobLogService {

    /**
     * 实现 framework 的 {@code JobLogWriter}：把调度记录转换为实体并落库。
     *
     * @param record 调度日志记录
     */
    void write(JobLogRecord record);

    /**
     * 同步写入一条调度日志。
     *
     * @param log 调度日志
     */
    void addJobLog(SysJobLog log);

    /**
     * 异步写入一条调度日志，写失败不影响调度主流程。
     *
     * @param log 调度日志
     */
    void addJobLogAsync(SysJobLog log);

    /**
     * 调度日志分页。
     *
     * @param query 分页与任务 / 状态筛选条件
     * @return 调度日志分页结果
     */
    IPage<SysJobLog> listPage(JobLogQueryDTO query);

    /**
     * 批量删除调度日志。
     *
     * @param ids 日志 ID 列表
     */
    void removeByIds(List<Long> ids);

    /**
     * 清空全部调度日志。
     */
    void removeAll();

    /**
     * 删除指定天数之前的调度日志。
     *
     * @param days 保留天数，最小按 1 天处理
     * @return 删除条数
     */
    int removeOlderThan(int days);

    /**
     * 删除指定任务的全部调度日志。
     *
     * @param jobId 任务 ID
     */
    void removeByJobId(Long jobId);
}
