package com.designer.admin.system.service.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.log.LoginLogQueryDTO;
import com.designer.admin.system.model.entity.log.SysLoginLog;

/**
 * 登录日志服务
 *
 * @author 雨夜终端
 */
public interface SysLoginLogService {

    /**
     * 异步写入登录日志。
     *
     * @param log 登录日志
     */
    void addLoginLogAsync(SysLoginLog log);

    /**
     * 登录日志分页。
     *
     * @param query 分页与账号 / 状态筛选条件
     * @return 登录日志分页结果
     */
    IPage<SysLoginLog> listPage(LoginLogQueryDTO query);

    /**
     * 删除指定天数之前的登录日志。
     *
     * @param days 保留天数，最小按 1 天处理
     * @return 删除条数
     */
    int removeOlderThan(int days);
}
