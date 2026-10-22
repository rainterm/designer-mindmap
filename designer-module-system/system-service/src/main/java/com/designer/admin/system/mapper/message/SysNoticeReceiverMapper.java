package com.designer.admin.system.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.message.SysNoticeReceiver;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 站内消息接收人数据访问层
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysNoticeReceiverMapper extends BaseMapper<SysNoticeReceiver> {

    /**
     * 批量写入接收人。
     *
     * <p>一条 INSERT 多组 VALUES，避免接收人多时逐条往返；Wrapper 表达不了这种批量写入。</p>
     *
     * @param noticeId 消息 ID（不能为空）
     * @param userIds  接收人用户 ID（调用方保证非空）
     * @return 写入行数
     */
    @Insert("<script>"
            + "INSERT INTO sys_notice_receiver (notice_id, user_id) VALUES "
            + "<foreach collection='userIds' item='userId' separator=','>(#{noticeId}, #{userId})</foreach>"
            + "</script>")
    int insertBatch(@Param("noticeId") Long noticeId, @Param("userIds") Collection<Long> userIds);
}
