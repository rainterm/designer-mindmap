package com.designer.admin.framework.message;

import com.designer.admin.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收集所有 {@link MessageChannelProvider}，按 type 索引。
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class MessageChannelRegistry {

    @Inject
    private AppContext appContext;

    private Map<String, MessageChannelProvider> providers = Collections.emptyMap();

    @Init
    public void init() {
        Map<String, MessageChannelProvider> map = new LinkedHashMap<>();
        appContext.subBeansOfType(MessageChannelProvider.class, p -> {
            MessageChannelProvider old = map.put(p.type(), p);
            if (old != null && old != p) {
                throw new IllegalStateException("重复的消息通道类型：" + p.type());
            }
            log.info("注册消息通道 Provider：{} ({})", p.type(), p.label());
        });
        this.providers = Collections.unmodifiableMap(map);
        if (map.isEmpty()) {
            log.warn("未发现任何 MessageChannelProvider 实现");
        }
    }

    public MessageChannelProvider getRequired(String type) {
        MessageChannelProvider p = providers.get(type);
        if (p == null) {
            throw new BizException("不支持的消息通道：" + type);
        }
        return p;
    }

    public boolean supports(String type) {
        return type != null && providers.containsKey(type);
    }

    public List<MessageChannelProvider> list() {
        return new ArrayList<>(providers.values());
    }
}
