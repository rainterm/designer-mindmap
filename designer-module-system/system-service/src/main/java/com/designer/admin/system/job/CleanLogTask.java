package com.designer.admin.system.job;

import com.designer.admin.framework.job.ScheduleConsts;
import com.designer.admin.system.service.job.SysJobLogService;
import com.designer.admin.system.service.log.SysLoginLogService;
import com.designer.admin.system.service.log.SysOperLogService;
import com.designer.admin.system.service.message.SysMessageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 内置日志清理任务。页面 invoke_target 示例：
 * <ul>
 *   <li>cleanLogTask.cleanAll(30) — 清理 30 天前的全部日志</li>
 *   <li>cleanLogTask.cleanOperLog(30)</li>
 *   <li>cleanLogTask.cleanLoginLog(30)</li>
 *   <li>cleanLogTask.cleanJobLog(30)</li>
 *   <li>cleanLogTask.cleanMessageRecord(30)</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@Component("cleanLogTask")
@Slf4j
public class CleanLogTask {

    @Inject
    private SysOperLogService operLogService;

    @Inject
    private SysLoginLogService loginLogService;

    @Inject
    private SysJobLogService jobLogService;

    @Inject
    private SysMessageRecordService messageRecordService;

    /** 清理全部日志表中超过保留天数的数据 */
    public void cleanAll(Integer days) {
        int keepDays = normalizeDays(days);
        int oper = operLogService.removeOlderThan(keepDays);
        int login = loginLogService.removeOlderThan(keepDays);
        int job = jobLogService.removeOlderThan(keepDays);
        int msg = messageRecordService.removeOlderThan(keepDays);
        log.info("[cleanLogTask] 清理完成 keepDays={} operLog={} loginLog={} jobLog={} msgRecord={}",
                keepDays, oper, login, job, msg);
    }

    public void cleanOperLog(Integer days) {
        int n = operLogService.removeOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 操作日志清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    public void cleanLoginLog(Integer days) {
        int n = loginLogService.removeOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 登录日志清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    public void cleanJobLog(Integer days) {
        int n = jobLogService.removeOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 调度日志清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    public void cleanMessageRecord(Integer days) {
        int n = messageRecordService.removeOlderThan(normalizeDays(days));
        log.info("[cleanLogTask] 消息记录清理 {} 条 (keepDays={})", n, normalizeDays(days));
    }

    private static int normalizeDays(Integer days) {
        return days == null || days < 1 ? ScheduleConsts.DEFAULT_LOG_KEEP_DAYS : days;
    }
}
