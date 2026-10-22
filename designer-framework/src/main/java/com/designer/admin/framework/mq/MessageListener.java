package com.designer.admin.framework.mq;

/**
 * 消息监听器。
 *
 * <p>实现方会在 MQ 的接收线程上回调，因此：</p>
 * <ul>
 *   <li>不要在这里做耗时操作（写库、HTTP 调用），否则会拖慢整个订阅；
 *       需要耗时处理时自行转入业务线程池。</li>
 *   <li>不要向回调线程抛出异常 —— 各实现都会捕获并记录，但抛异常会让当前这条消息被丢弃。</li>
 * </ul>
 *
 * @author 雨夜终端
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * 收到消息。
     *
     * @param topic   主题
     * @param payload 消息体
     */
    void onMessage(String topic, String payload);
}
