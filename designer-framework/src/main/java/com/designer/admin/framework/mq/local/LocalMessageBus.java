package com.designer.admin.framework.mq.local;

import com.designer.admin.framework.mq.MessageBus;
import com.designer.admin.framework.mq.MessageListener;
import com.designer.admin.framework.mq.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.util.RunUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 进程内消息总线（单机实现）。
 *
 * <p><b>用途</b>：本地开发、单测，以及明确只有一个实例的部署。
 * 它同时也是 {@link MessageBus} SPI 的参考实现 —— 新增 MQ 实现时
 * 可以对照它理解「订阅句柄」与「关闭语义」的边界。</p>
 *
 * <p><b>不要在多实例环境使用</b>：消息只在本进程内投递，另一个实例上的订阅者
 * 永远收不到。这不会报错，只会表现为「有时收不到消息」，是最难排查的一类问题；
 * 因此切换实现只能通过显式配置 {@code designer.mq.type=local}，不会自动降级。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class LocalMessageBus implements MessageBus {

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "local";
    }

    @Override
    public void publish(String topic, String payload) {
        List<MessageListener> topicListeners = listeners.get(topic);
        if (topicListeners == null || topicListeners.isEmpty()) {
            return;
        }
        for (MessageListener listener : topicListeners) {
            // 异步投递与 Redis 实现保持同一语义：发布方不被监听器阻塞，
            // 也让「监听器抛异常拖垮业务调用」这种问题在两种实现下表现一致
            RunUtil.asyncAndTry(() -> {
                try {
                    listener.onMessage(topic, payload);
                } catch (Exception e) {
                    log.warn("消息监听器处理失败 topic={}", topic, e);
                }
            });
        }
    }

    @Override
    public Subscription subscribe(String topic, MessageListener listener) {
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
        return new LocalSubscription(topic, listener);
    }

    private final class LocalSubscription implements Subscription {

        private final String topic;
        private final MessageListener listener;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private LocalSubscription(String topic, MessageListener listener) {
            this.topic = topic;
            this.listener = listener;
        }

        @Override
        public String topic() {
            return topic;
        }

        @Override
        public boolean isActive() {
            return !closed.get();
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            CopyOnWriteArrayList<MessageListener> topicListeners = listeners.get(topic);
            if (topicListeners == null) {
                return;
            }
            topicListeners.remove(listener);
            if (topicListeners.isEmpty()) {
                // 移除空列表：避免长期运行积累大量「曾经订阅过」的 topic 键
                listeners.remove(topic, topicListeners);
            }
        }
    }
}
