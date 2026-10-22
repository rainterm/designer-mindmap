package com.designer.admin.framework.mq;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 消息总线入口：按配置选定一种 {@link MessageBus} 实现并对外提供。
 *
 * <p><b>为什么不让业务直接注入 {@code MessageBus}</b>：容器里可能同时存在多个实现
 * （Redis、Local、后续的 RabbitMQ），按类型注入会变成「谁先注册谁生效」，
 * 换实现时行为不可预期。这里用 {@code designer.mq.type} 显式选择，
 * 并在启动时校验配置，配错立即失败而不是静默退化成单机。</p>
 *
 * <p><b>扩展方式</b>：新增实现类标注 {@code @Component} 即可出现在候选列表里，
 * 不需要改这个类；启动日志会打印所有可用实现，配置写错时也能直接看到可选项。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class MessageBusManager {

    /** 实现选择配置项 */
    private static final String TYPE_CONFIG_KEY = "designer.mq.type";

    /**
     * 默认实现。
     *
     * <p>选 Redis 而不是 local：本项目的会话、限流、验证码都已经依赖 Redis，
     * 多实例部署是既定前提，默认值不该是「多实例下会静默丢消息」的那个。</p>
     */
    private static final String DEFAULT_TYPE = "redis";

    private MessageBus bus;

    @Init(index = 0)
    public void init() {
        String type = Solon.cfg().get(TYPE_CONFIG_KEY, DEFAULT_TYPE);
        List<MessageBus> candidates = Solon.context().getBeansOfType(MessageBus.class);

        for (MessageBus candidate : candidates) {
            if (type.equalsIgnoreCase(candidate.type())) {
                bus = candidate;
                break;
            }
        }

        if (bus == null) {
            Set<String> available = new TreeSet<>();
            for (MessageBus candidate : candidates) {
                available.add(candidate.type());
            }
            throw new IllegalStateException("未找到消息总线实现：" + TYPE_CONFIG_KEY + "=" + type
                    + "，当前可用实现 " + new ArrayList<>(available)
                    + "（新增实现只需实现 MessageBus 并标注 @Component）");
        }

        log.info("消息总线已启用：{}", bus.type());
    }

    @Destroy
    public void stop() {
        if (bus != null) {
            try {
                bus.close();
            } catch (Exception e) {
                log.warn("消息总线关闭失败 type={}", bus.type(), e);
            }
        }
    }

    /**
     * 当前生效的实现。
     *
     * @return 消息总线实现
     */
    public MessageBus bus() {
        if (bus == null) {
            // 正常不会发生：init 已保证非空。这里防的是被手动 new 出来（如单测）后直接使用
            throw new IllegalStateException("消息总线尚未初始化");
        }
        return bus;
    }

    /**
     * 当前实现标识。
     *
     * @return 实现标识
     */
    public String type() {
        return bus().type();
    }

    /**
     * 发布消息，语义见 {@link MessageBus#publish(String, String)}。
     *
     * @param topic   主题
     * @param payload 消息体
     */
    public void publish(String topic, String payload) {
        bus().publish(topic, payload);
    }

    /**
     * 订阅主题，语义见 {@link MessageBus#subscribe(String, MessageListener)}。
     *
     * @param topic    主题
     * @param listener 监听器
     * @return 订阅句柄
     */
    public Subscription subscribe(String topic, MessageListener listener) {
        return bus().subscribe(topic, listener);
    }
}
