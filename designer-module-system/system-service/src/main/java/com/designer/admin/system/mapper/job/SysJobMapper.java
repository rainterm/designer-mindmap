package com.designer.admin.system.mapper.job;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.job.SysJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务数据访问。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {
}