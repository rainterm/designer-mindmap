package com.designer.admin.module.agent.util;

import org.noear.solon.web.sse.SseEmitter;
import org.noear.solon.web.sse.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 安全发送与完结，供会话 / 工作流 / 知识库复用。
 *
 * @author 雨夜终端
 */
public final class AgentSseSupport {

    private static final Logger log = LoggerFactory.getLogger(AgentSseSupport.class);

    private AgentSseSupport() {
    }

    public static void safeSend(SseEmitter emitter, AtomicBoolean closed, String event, String json) {
        if (closed.get() || emitter == null) {
            return;
        }
        try {
            emitter.send(new SseEvent().name(event).data(json));
        } catch (Exception e) {
            // 客户端断开时常见，标记关闭即可
            closed.set(true);
            log.debug("SSE 发送失败 event={}", event, e);
        }
    }

    public static void completeQuietly(SseEmitter emitter, AtomicBoolean closed) {
        if (closed.get() || emitter == null) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception e) {
            // 客户端已断开可忽略
            log.debug("SSE complete 忽略", e);
        } finally {
            closed.set(true);
        }
    }
}
