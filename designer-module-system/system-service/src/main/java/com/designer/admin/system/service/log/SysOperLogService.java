package com.designer.admin.system.service.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.framework.log.OperLogRecord;
import com.designer.admin.framework.log.OperLogWriter;
import com.designer.admin.system.model.dto.log.OperLogQueryDTO;
import com.designer.admin.system.model.entity.log.SysOperLog;

/**
 * 操作日志服务，同时实现 framework {@link OperLogWriter} SPI。
 *
 * @author 雨夜终端
 */
public interface SysOperLogService {

    /**
     * 实现 framework 的 {@code OperLogWriter}：异步写入操作日志。
     *
     * @param record 操作日志记录
     */
    void writeAsync(OperLogRecord record);

    /**
     * 异步写入一条操作日志。
     *
     * @param log 操作日志
     */
    void addOperLogAsync(SysOperLog log);

    /**
     * 操作日志分页。
     *
     * @param query 分页与标题 / 操作人 / 状态筛选条件
     * @return 操作日志分页结果
     */
    IPage<SysOperLog> listPage(OperLogQueryDTO query);

    /**
     * 删除指定天数之前的操作日志。
     *
     * @param days 保留天数，最小按 1 天处理
     * @return 删除条数
     */
    int removeOlderThan(int days);
}
