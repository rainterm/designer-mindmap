package com.designer.admin.system.service.message.impl;

import com.designer.admin.common.constant.SystemConsts;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.message.InboxSink;
import com.designer.admin.framework.message.NoticeSseHub;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.constant.SysNoticeConsts;
import com.designer.admin.system.mapper.message.SysNoticeMapper;
import com.designer.admin.system.mapper.message.SysNoticeReadMapper;
import com.designer.admin.system.mapper.message.SysNoticeReceiverMapper;
import com.designer.admin.system.mapper.rbac.SysUserMapper;
import com.designer.admin.system.model.entity.message.SysNotice;
import com.designer.admin.system.model.entity.message.SysNoticeRead;
import com.designer.admin.system.model.entity.rbac.SysUser;
import com.designer.admin.system.model.vo.message.NoticePushVO;
import com.designer.admin.system.service.message.SysNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站内消息：通道 inbox 投递落库 + 顶部消息中心 + SSE 推送；实现 framework {@link InboxSink}。
 *
 * <p><b>可见性</b>：不再用 {@code receivers} 逗号串做过滤 —— 那样每次查收件箱都要全表加载后内存筛选。
 * 全体消息记在 {@code sys_notice.receiver_all}，定向消息落 {@code sys_notice_receiver}，
 * 分页与未读数都由数据库按 {@code (user_id, notice_id)} 索引完成。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class SysNoticeServiceImpl implements SysNoticeService, InboxSink {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Db
    private SysNoticeMapper noticeMapper;
    @Db
    private SysNoticeReceiverMapper noticeReceiverMapper;
    @Db
    private SysNoticeReadMapper noticeReadMapper;
    @Db
    private SysUserMapper userMapper;

    @Inject
    private NoticeSseHub noticeSseHub;

    @Override
    public Long publish(String title, String content, String receivers) {
        return publishInbox(title, content, receivers, null);
    }

    @Override
    public Long publish(String title, String content, String receivers, String link) {
        return publishInbox(title, content, receivers, link);
    }

    @Override
    @Transaction
    public Long publishInbox(String title, String content, String receivers, String link) {
        String rawReceivers = receivers == null ? "" : receivers.trim();
        boolean all = rawReceivers.isEmpty() || SysNoticeConsts.RECEIVERS_ALL.equals(rawReceivers);
        List<Long> receiverIds = all
                ? List.of()
                : resolveReceiverUserIds(splitReceivers(rawReceivers));

        SysNotice notice = new SysNotice();
        notice.setTitle(title);
        notice.setContent(content == null ? "" : content);
        notice.setStatus(SystemConsts.STATUS_ENABLE);
        notice.setReceiverAll(all ? 1 : 0);
        // 原文保留，便于后台排查「这条消息当时发给了谁」；查询不依赖它
        notice.setReceivers(rawReceivers);
        if (link != null && !link.isBlank()) {
            notice.setRemark(SysNoticeConsts.LINK_PREFIX + link.trim());
            notice.setLink(link.trim());
        } else {
            notice.setRemark("站内通道投递");
        }
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);

        if (!receiverIds.isEmpty()) {
            noticeReceiverMapper.insertBatch(notice.getId(), receiverIds);
        } else if (!all) {
            log.warn("站内消息没有匹配到有效接收人，仅落库不推送 noticeId={} receivers={}", notice.getId(), rawReceivers);
        }

        pushNewNotice(notice, all, receiverIds);
        return notice.getId();
    }

    @Override
    @Transaction
    public Long publishInbox(String title, String content, String receivers) {
        return publishInbox(title, content, receivers, null);
    }

    @Override
    public IPage<SysNotice> listInboxPage(PageQuery query) {
        Long userId = LoginHelper.getUserId();
        Page<SysNotice> page = query.toPage();
        IPage<SysNotice> result = noticeMapper.selectPage(page,
                visibleWrapper(userId).orderByDesc(SysNotice::getId));

        List<SysNotice> records = result.getRecords();
        Set<Long> readIds = readNoticeIds(userId, records.stream().map(SysNotice::getId).toList());
        for (SysNotice n : records) {
            n.setReadFlag(readIds.contains(n.getId()));
            n.setLink(parseLink(n.getRemark()));
        }
        return result;
    }

    @Override
    public long countUnread() {
        Long userId = LoginHelper.getUserId();
        Long count = noticeMapper.selectCount(visibleWrapper(userId)
                .notExists("SELECT 1 FROM sys_notice_read rd "
                        + "WHERE rd.notice_id = sys_notice.id AND rd.user_id = {0}", userId));
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(Long noticeId) {
        if (noticeId == null) {
            throw new BizException("消息ID不能为空");
        }
        Long userId = LoginHelper.getUserId();
        // 一次索引查询同时完成「存在 + 已发布 + 对本人可见」，不再加载整条消息到内存判断
        SysNotice notice = noticeMapper.selectOne(visibleWrapper(userId).eq(SysNotice::getId, noticeId));
        if (notice == null) {
            throw new BizException("消息不存在或已关闭");
        }
        noticeReadMapper.insertIgnore(noticeId, userId, LocalDateTime.now());
    }

    @Override
    public int markAllRead() {
        return noticeReadMapper.insertUnreadForUser(LoginHelper.getUserId(), LocalDateTime.now());
    }

    /**
     * 当前用户可见消息的条件：已发布 + （全体 或 命中接收人表）。
     *
     * <p>两处都存在 {@code (user_id, notice_id)} 索引，EXISTS 只做索引探测，不产生临时表。</p>
     */
    private LambdaQueryWrapper<SysNotice> visibleWrapper(Long userId) {
        return new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, SystemConsts.STATUS_ENABLE)
                .and(w -> w.eq(SysNotice::getReceiverAll, 1)
                        .or()
                        .exists("SELECT 1 FROM sys_notice_receiver r "
                                + "WHERE r.notice_id = sys_notice.id AND r.user_id = {0}", userId));
    }

    private void pushNewNotice(SysNotice notice, boolean all, List<Long> receiverIds) {
        String link = notice.getLink() != null ? notice.getLink() : parseLink(notice.getRemark());
        NoticePushVO payload = NoticePushVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(truncate(notice.getContent(), 200))
                .createTime(notice.getCreateTime() == null ? "" : TIME_FMT.format(notice.getCreateTime()))
                .link(link)
                .build();
        String json = JsonUtil.toJson(payload);

        if (all) {
            noticeSseHub.pushToAllOnline("notice", json);
            return;
        }
        if (receiverIds.isEmpty()) {
            return;
        }
        noticeSseHub.pushToUsers(receiverIds, "notice", json);
    }

    private static List<String> splitReceivers(String receivers) {
        return Arrays.stream(receivers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * receivers 支持用户 ID 或登录账号；审批模块传 ID，消息通道可能传账号。
     */
    private List<Long> resolveReceiverUserIds(List<String> tokens) {
        Set<Long> ids = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        for (String token : tokens) {
            try {
                ids.add(Long.valueOf(token));
            } catch (NumberFormatException e) {
                names.add(token);
            }
        }
        if (!names.isEmpty()) {
            userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getUsername, names)
                            .eq(SysUser::getStatus, 1)
                            .select(SysUser::getId))
                    .stream()
                    .map(SysUser::getId)
                    .filter(id -> id != null)
                    .forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, ids)
                        .eq(SysUser::getStatus, 1)
                        .select(SysUser::getId))
                .stream()
                .map(SysUser::getId)
                .toList();
    }

    private static String parseLink(String remark) {
        if (remark == null || remark.isBlank() || !remark.startsWith(SysNoticeConsts.LINK_PREFIX)) {
            return null;
        }
        String link = remark.substring(SysNoticeConsts.LINK_PREFIX.length()).trim();
        return link.isEmpty() ? null : link;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }

    private Set<Long> readNoticeIds(Long userId, List<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return Set.of();
        }
        return noticeReadMapper.selectList(new LambdaQueryWrapper<SysNoticeRead>()
                        .eq(SysNoticeRead::getUserId, userId)
                        .in(SysNoticeRead::getNoticeId, noticeIds))
                .stream()
                .map(SysNoticeRead::getNoticeId)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
