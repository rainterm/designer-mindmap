package com.designer.admin.framework.mq;

/**
 * 消息总线（发布 / 订阅）抽象。
 *
 * <p><b>为什么需要这一层</b>：跨实例广播原本直接写在 SSE 里（调用 Redis Pub/Sub），
 * 于是「消息怎么发」和「消息发给谁」耦合在一起。缓存失效、配置变更广播、领域事件
 * 这些场景都要用同一套能力，再各写一遍 Redis 调用就会出现多套重连线程、多种消息格式，
 * 将来换成 RabbitMQ / Kafka 时也无处下手。</p>
 *
 * <p><b>语义约定</b>（实现必须遵守，调用方可以依赖）：</p>
 * <ul>
 *   <li><b>即发即弃</b>：不做持久化、不做重投，消费者不在线就收不到。
 *       需要「可靠投递」时应换用带持久化的实现，并在业务侧配合本地消息表（outbox）。</li>
 *   <li><b>不保证顺序</b>：同一 topic 的先后顺序不做承诺；需要顺序的业务应自行带版本号。</li>
 *   <li><b>至少一次到「本进程」</b>：订阅方在自己的进程内会收到，但不保证只收到一次 ——
 *       Redis Pub/Sub 会把消息回传给发布者，所以「本机直接投递 + 广播」会造成重复，
 *       发布方不应再额外本地推送。</li>
 *   <li><b>payload 是字符串</b>：框架不假设 JSON / Java 序列化，由调用方约定格式。
 *       这样换 MQ 时不需要处理跨语言序列化不兼容的问题。</li>
 * </ul>
 *
 * <p><b>如何新增一种 MQ</b>：实现本接口的 {@link #type()} 返回一个唯一标识
 * （如 {@code rabbit}），标注 {@code @Component}，然后在配置里把
 * {@code designer.mq.type} 改为该值即可，无需改动调用方。实现只会在被选中时初始化。</p>
 *
 * @author 雨夜终端
 */
public interface MessageBus extends AutoCloseable {

    /**
     * 实现标识，与 {@code designer.mq.type} 的取值对应。
     *
     * @return 唯一标识，例如 {@code redis} / {@code local} / {@code rabbit}
     */
    String type();

    /**
     * 发布消息。
     *
     * @param topic   主题，调用方自行约定命名（建议 {@code 应用:业务:事件}）
     * @param payload 消息体，格式由调用方约定
     * @throws MessageBusException 发布失败（连接不可用等）
     */
    void publish(String topic, String payload);

    /**
     * 订阅主题。
     *
     * <p>返回的 {@link Subscription} 是<b>一次性</b>的：重复调用会各自建立订阅，
     * 同一监听器会收到多份消息。调用方应持有返回值并在销毁时关闭。</p>
     *
     * @param topic    主题
     * @param listener 消息监听器
     * @return 订阅句柄
     */
    Subscription subscribe(String topic, MessageListener listener);

    /**
     * 释放实现占用的资源（订阅线程、连接等）。
     */
    @Override
    default void close() {
        // 默认无资源
    }
}
