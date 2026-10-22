package com.designer.admin.system.mapper.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.log.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
