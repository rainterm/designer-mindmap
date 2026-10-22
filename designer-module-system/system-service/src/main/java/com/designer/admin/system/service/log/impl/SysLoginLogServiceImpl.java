package com.designer.admin.system.service.log.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.noear.solon.core.util.RunUtil;
import com.designer.admin.system.mapper.log.SysLoginLogMapper;
import com.designer.admin.system.model.dto.log.LoginLogQueryDTO;
import com.designer.admin.system.model.entity.log.SysLoginLog;
import com.designer.admin.system.service.log.SysLoginLogService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;

/**
 * 登录日志服务
 *
 * @author 雨夜终端
 */
@Component
public class SysLoginLogServiceImpl implements SysLoginLogService {

    @Db
    private SysLoginLogMapper loginLogMapper;

    @Override
    public void addLoginLogAsync(SysLoginLog log) {
        RunUtil.asyncAndTry(() -> loginLogMapper.insert(log));
    }

    @Override
    public IPage<SysLoginLog> listPage(LoginLogQueryDTO query) {
        LambdaQueryWrapper<SysLoginLog> w = new LambdaQueryWrapper<SysLoginLog>()
                .like(query.getUsername() != null && !query.getUsername().isEmpty(),
                        SysLoginLog::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                .orderByDesc(SysLoginLog::getId);
        return loginLogMapper.selectPage(query.toPage(), w);
    }

    @Override
    public int removeOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return loginLogMapper.delete(new LambdaQueryWrapper<SysLoginLog>()
                .lt(SysLoginLog::getLoginTime, before));
    }
}
