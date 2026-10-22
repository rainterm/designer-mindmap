package com.designer.admin.system.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.message.SysMessageRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息发送记录数据访问。
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysMessageRecordMapper extends BaseMapper<SysMessageRecord> {
}