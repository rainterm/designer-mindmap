package com.designer.admin.system.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.message.SysNoticeRead;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 通知公告已读 Mapper
 *
 * @author 雨夜终端
 */
@Mapper
public interface SysNoticeReadMapper extends BaseMapper<SysNoticeRead> {

    /**
     * 写入已读记录，已存在则忽略（幂等）。
     *
     * <p>不用「先 selectCount 再 insert」：唯一键 {@code uk_sys_notice_read} 已经能防重，
     * 先查后写反而会在并发双击时撞键报错。</p>
     *
     * @param noticeId 消息 ID
     * @param userId   用户 ID
     * @param readTime 阅读时间
     * @return 实际写入行数（已读时为 0）
     */
    @Insert("INSERT IGNORE INTO sys_notice_read (notice_id, user_id, read_time) "
            + "VALUES (#{noticeId}, #{userId}, #{readTime})")
    int insertIgnore(@Param("noticeId") Long noticeId,
                     @Param("userId") Long userId,
                     @Param("readTime") LocalDateTime readTime);

    /**
     * 把当前用户所有未读消息一次性标记为已读。
     *
     * <p>一条 {@code INSERT ... SELECT} 完成「筛可见消息 + 排除已读 + 批量写入」，
     * 不在应用层把消息全量加载后再逐条 insert。</p>
     *
     * <p>可见性条件必须与收件箱查询保持一致：全体消息看 {@code receiver_all}，
     * 定向消息看 {@code sys_notice_receiver}。</p>
     *
     * @param userId   用户 ID
     * @param readTime 阅读时间
     * @return 新增的已读记录数
     */
    @Insert("INSERT INTO sys_notice_read (notice_id, user_id, read_time) "
            + "SELECT n.id, #{userId}, #{readTime} FROM sys_notice n "
            + "WHERE n.status = 1 AND n.deleted = 0 "
            + "AND (n.receiver_all = 1 OR EXISTS ("
            + "    SELECT 1 FROM sys_notice_receiver r WHERE r.notice_id = n.id AND r.user_id = #{userId})) "
            + "AND NOT EXISTS ("
            + "    SELECT 1 FROM sys_notice_read rd WHERE rd.notice_id = n.id AND rd.user_id = #{userId})")
    int insertUnreadForUser(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
}
