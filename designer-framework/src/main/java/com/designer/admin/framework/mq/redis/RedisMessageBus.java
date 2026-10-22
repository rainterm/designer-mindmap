package com.designer.admin.framework.mq.redis;

import com.designer.admin.framework.mq.MessageBus;
import com.designer.admin.framework.mq.MessageBusException;
import com.designer.admin.framework.mq.MessageListener;
import com.designer.admin.framework.mq.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Redis Pub/Sub 的消息总线（默认实现）。
 *
 * <p><b>连接代价</b>：Redis 的 SUBSCRIBE 是阻塞命令，<b>一条活跃订阅会占住一个连接</b>。
 * 因此这里不共享订阅连接 —— 不同 topic 各自一条线程 + 连接，换来的是
 * 「一个慢消费者/一个 topic 卡住」不会影响其它 topic。代价是 topic 数量要与
 * {@code sa-token-dao-redis.maxTotal} 匹配，不要把 topic 做成按用户维度的高基数键。</p>
 *
 * <p><b>断线重连</b>：订阅线程在异常后按固定间隔重试。Redis 重启期间发布的
 * 消息不会补发（Pub/Sub 无持久化），这是选型本身的语义，不是这里的缺陷；
 * 需要补发请换用带持久化的实现。</p>
 *
 * <p><b>线程性质</b>：订阅线程是守护线程。它阻塞在 socket 读上，
 * 不应该因为「还有一个订阅在」而让进程无法退出；退出时由
 * {@link Subscription#close()} 主动 unsubscribe 唤醒。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class RedisMessageBus implements MessageBus {

    /** 订阅断开后的重试间隔（毫秒） */
    private static final long RETRY_DELAY_MILLIS = 5_000L;

    /** 当前存活的订阅：关闭总线时统一回收，避免泄漏线程与连接 */
    private final Set<RedisSubscription> subscriptions = ConcurrentHashMap.newKeySet();

    @Inject
    private RedisClient redisClient;

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public void publish(String topic, String payload) {
        try {
            redisClient.getBus().publish(topic, payload);
        } catch (RuntimeException e) {
            // 统一包装成框架异常：调用方不必依赖 Jedis 的异常类型，
            // 将来把这个实现换成 RabbitMQ 时 catch 语句不用改
            throw new MessageBusException("消息发布失败 topic=" + topic, e);
        }
    }

    @Override
    public Subscription subscribe(String topic, MessageListener listener) {
        RedisSubscription subscription = new RedisSubscription(topic, listener);
        subscriptions.add(subscription);
        subscription.start();
        return subscription;
    }

    @Override
    public void close() {
        for (RedisSubscription subscription : subscriptions) {
            subscription.close();
        }
        subscriptions.clear();
    }

    private void remove(RedisSubscription subscription) {
        subscriptions.remove(subscription);
    }

    /**
     * 单条 topic 的订阅：一条守护线程 + 一次阻塞 subscribe。
     */
    private final class RedisSubscription implements Subscription {

        private final String topic;
        private final MessageListener listener;

        /** 当前 JedisPubSub：重连时重建，关闭时用它解除阻塞 */
        private volatile JedisPubSub subscriber;

        /** 已关闭标记：阻止重连循环继续、也让 close 幂等 */
        private volatile boolean closed;

        private Thread thread;

        private RedisSubscription(String topic, MessageListener listener) {
            this.topic = topic;
            this.listener = listener;
        }

        private void start() {
            thread = new Thread(this::loop, "mq-redis-" + topic);
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public String topic() {
            return topic;
        }

        @Override
        public boolean isActive() {
            return !closed;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            JedisPubSub current = subscriber;
            if (current != null) {
                try {
                    // 从另一个线程取消订阅，才能让阻塞中的 subscribe 返回
                    current.unsubscribe();
                } catch (Exception e) {
                    log.debug("取消订阅失败 topic={}", topic, e);
                }
            }

            // 连接/线程都已被唤醒，不再需要中断；join 只是为了让关闭顺序更可预期
            if (thread != null && thread != Thread.currentThread()) {
                try {
                    thread.join(RETRY_DELAY_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            remove(this);
        }

        private void loop() {
            while (!closed) {
                // 每次重连都新建 JedisPubSub：上一个实例可能残留「已订阅」的内部状态，
                // 复用会导致重连成功却不再回调
                JedisPubSub current = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            listener.onMessage(channel, message);
                        } catch (Exception e) {
                            // 监听器异常不能让订阅线程退出，否则整个 topic 静默失联
                            log.warn("消息监听器处理失败 topic={}", channel, e);
                        }
                    }
                };
                subscriber = current;

                try {
                    redisClient.open(session -> session.subscribe(current, topic));
                } catch (Exception e) {
                    if (closed) {
                        return;
                    }
                    log.warn("Redis 订阅中断，{} ms 后重试 topic={}", RETRY_DELAY_MILLIS, topic, e);
                }

                if (closed) {
                    return;
                }
                // 正常返回（被取消订阅）或异常都退避一下，避免异常场景变成忙循环
                try {
                    Thread.sleep(RETRY_DELAY_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
