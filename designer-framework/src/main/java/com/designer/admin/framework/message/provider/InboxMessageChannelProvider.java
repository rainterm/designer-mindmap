package com.designer.admin.framework.message.provider;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.message.InboxSink;
import com.designer.admin.framework.message.MessageChannelConsts;
import com.designer.admin.framework.message.MessageChannelProvider;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.framework.spi.ConfigFieldMeta;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 站内通知通道：写入消息中心，由顶部铃铛投递给登录用户。
 *
 * @author 雨夜终端
 */
@Component
public class InboxMessageChannelProvider implements MessageChannelProvider {

    @Inject
    private InboxSink inboxSink;

    @Override
    public String type() {
        return MessageChannelConsts.TYPE_INBOX;
    }

    @Override
    public String label() {
        return "站内通知";
    }

    @Override
    public List<ConfigFieldMeta> configFields() {
        return Collections.emptyList();
    }

    @Override
    public void validateConfig(Map<String, Object> props) {
        // 无外部凭据
    }

    @Override
    public void send(MessagePayload payload, MessageRuntimeConfig config) {
        if (payload == null) {
            throw new BizException("消息内容不能为空");
        }
        String title = payload.getTitle();
        if (title == null || title.isBlank()) {
            throw new BizException("站内通知标题不能为空");
        }
        inboxSink.publish(title.trim(), payload.getContent(), resolveReceivers(payload.getTo()));
    }

    private String resolveReceivers(List<String> to) {
        if (to == null || to.isEmpty()) {
            return "";
        }
        boolean broadcast = to.stream().anyMatch(s -> s != null
                && ("*".equals(s.trim()) || "all".equalsIgnoreCase(s.trim())));
        if (broadcast) {
            return "";
        }
        return to.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }
}
