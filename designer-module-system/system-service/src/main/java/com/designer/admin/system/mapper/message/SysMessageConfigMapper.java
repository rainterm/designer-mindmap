package com.designer.admin.system.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.message.SysMessageConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息通道配置数据访问。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysMessageConfigMapper extends BaseMapper<SysMessageConfig> {
}