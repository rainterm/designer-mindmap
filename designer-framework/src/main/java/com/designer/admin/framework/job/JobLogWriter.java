package com.designer.admin.framework.job;

/**
 * 定时任务日志写入 SPI：由 system 模块实现。
 *
 * @author 雨夜终端
 */
public interface JobLogWriter {

    /**
     * 写入一条调度日志。
     *
     * @param record 调度日志记录
     */
    void write(JobLogRecord record);
}
