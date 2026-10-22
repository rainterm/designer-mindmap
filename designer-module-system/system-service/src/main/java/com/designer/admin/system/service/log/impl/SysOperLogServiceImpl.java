package com.designer.admin.system.service.log.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.noear.solon.core.util.RunUtil;
import com.designer.admin.framework.log.OperLogRecord;
import com.designer.admin.framework.log.OperLogWriter;
import com.designer.admin.system.mapper.log.SysOperLogMapper;
import com.designer.admin.system.model.dto.log.OperLogQueryDTO;
import com.designer.admin.system.model.entity.log.SysOperLog;
import com.designer.admin.system.service.log.SysOperLogService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;

/**
 * 操作日志服务，同时实现 framework {@link OperLogWriter} SPI。
 *
 * @author 雨夜终端
 */
@Component
public class SysOperLogServiceImpl implements SysOperLogService, OperLogWriter {

    @Db
    private SysOperLogMapper operLogMapper;

    @Override
    public void writeAsync(OperLogRecord record) {
        if (record == null) {
            return;
        }
        SysOperLog log = new SysOperLog();
        log.setTitle(record.getTitle());
        log.setBusinessType(record.getBusinessType());
        log.setMethod(record.getMethod());
        log.setRequestMethod(record.getRequestMethod());
        log.setOperName(record.getOperName());
        log.setOperUrl(record.getOperUrl());
        log.setOperIp(record.getOperIp());
        log.setOperParam(record.getOperParam());
        log.setJsonResult(record.getJsonResult());
        log.setStatus(record.getStatus());
        log.setErrorMsg(record.getErrorMsg());
        log.setCostTime(record.getCostTime());
        log.setOperTime(record.getOperTime());
        addOperLogAsync(log);
    }

    @Override
    public void addOperLogAsync(SysOperLog log) {
        // RunUtil.asyncAndTry：丢到 Solon 并行执行器执行并吞掉异常（JDK21+ 可启用虚拟线程）
        RunUtil.asyncAndTry(() -> operLogMapper.insert(log));
    }

    @Override
    public IPage<SysOperLog> listPage(OperLogQueryDTO query) {
        return operLogMapper.selectPage(query.toPage(),
                buildWrapper(query.getTitle(), query.getOperName(), query.getStatus()));
    }

    private LambdaQueryWrapper<SysOperLog> buildWrapper(String title, String operName, Integer status) {
        return new LambdaQueryWrapper<SysOperLog>()
                .like(title != null && !title.isEmpty(), SysOperLog::getTitle, title)
                .like(operName != null && !operName.isEmpty(), SysOperLog::getOperName, operName)
                .eq(status != null, SysOperLog::getStatus, status)
                .orderByDesc(SysOperLog::getId);
    }

    @Override
    public int removeOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return operLogMapper.delete(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getOperTime, before));
    }
}
