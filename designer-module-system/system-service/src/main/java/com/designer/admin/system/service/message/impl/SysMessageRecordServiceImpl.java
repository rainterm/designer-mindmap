package com.designer.admin.system.service.message.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import org.noear.solon.core.util.RunUtil;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRecordWriter;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.constant.SysMessageConsts;
import com.designer.admin.system.mapper.message.SysMessageRecordMapper;
import com.designer.admin.system.model.dto.message.MessageRecordQueryDTO;
import com.designer.admin.system.model.entity.message.SysMessageRecord;
import com.designer.admin.system.service.message.SysMessageRecordService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息发送记录；实现 framework {@link MessageRecordWriter}。
 *
 * @author 雨夜终端
 */
@Component
public class SysMessageRecordServiceImpl implements SysMessageRecordService, MessageRecordWriter {

    @Db
    private SysMessageRecordMapper recordMapper;

    @Override
    public IPage<SysMessageRecord> listPage(MessageRecordQueryDTO query) {
        return recordMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysMessageRecord>()
                .eq(notBlank(query.getChannelType()), SysMessageRecord::getChannelType, query.getChannelType())
                .like(notBlank(query.getTitle()), SysMessageRecord::getTitle, query.getTitle())
                .like(notBlank(query.getReceivers()), SysMessageRecord::getReceivers, query.getReceivers())
                .like(notBlank(query.getSendBy()), SysMessageRecord::getSendBy, query.getSendBy())
                .eq(query.getStatus() != null, SysMessageRecord::getStatus, query.getStatus())
                .orderByDesc(SysMessageRecord::getId));
    }

    @Override
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的记录");
        }
        recordMapper.deleteByIds(ids);
    }

    @Override
    public void removeAll() {
        recordMapper.delete(new LambdaQueryWrapper<SysMessageRecord>().ge(SysMessageRecord::getId, 0));
    }

    @Override
    public int removeOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return recordMapper.delete(new LambdaQueryWrapper<SysMessageRecord>()
                .lt(SysMessageRecord::getSendTime, before));
    }

    @Override
    public void recordAsync(MessageRuntimeConfig config, MessagePayload payload,
                            boolean success, String errorMsg, long costTime) {
        SysMessageRecord record = buildRecord(config, payload, success, errorMsg, costTime);
        RunUtil.asyncAndTry(() -> recordMapper.insert(record));
    }

    private SysMessageRecord buildRecord(MessageRuntimeConfig config, MessagePayload payload,
                                         boolean success, String errorMsg, long costTime) {
        SysMessageRecord record = new SysMessageRecord();
        record.setConfigId(config == null ? null : config.getConfigId());
        record.setConfigName(config == null ? "" : nullToEmpty(config.getConfigName()));
        record.setChannelType(config == null ? "" : nullToEmpty(config.getChannelType()));
        record.setReceivers(join(payload == null ? null : payload.getTo()));
        record.setTitle(payload == null ? "" : nullToEmpty(payload.getTitle()));
        String content = payload == null ? "" : nullToEmpty(payload.getContent());
        if (content.length() > SysMessageConsts.MAX_CONTENT_LENGTH) {
            content = content.substring(0, SysMessageConsts.MAX_CONTENT_LENGTH) + "...";
        }
        record.setContent(content);
        record.setHtml(payload != null && payload.isHtml() ? 1 : 0);
        record.setStatus(success ? 1 : 0);
        record.setErrorMsg(errorMsg == null ? "" : (errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg));
        record.setCostTime(costTime);
        record.setSendBy(currentUsername());
        record.setSendTime(LocalDateTime.now());
        return record;
    }

    private static String join(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).collect(Collectors.joining(","));
    }

    private static String currentUsername() {
        try {
            if (!StpUtil.isLogin()) {
                return "";
            }
            return nullToEmpty(LoginHelper.getUsername());
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
