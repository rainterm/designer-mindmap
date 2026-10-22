package com.designer.admin.framework.mq;

/**
 * 订阅句柄：关闭即取消订阅。
 *
 * <p>做成显式句柄而不是「订阅了就一直有效」，是因为订阅会占用连接或线程
 * （Redis 的 SUBSCRIBE 是阻塞命令，一条订阅至少占用一个连接）。
 * 业务模块可能在运行时动态订阅主题，没有句柄就永远回收不掉。</p>
 *
 * <p>实现必须保证 {@link #close()} 幂等：重复调用不报错。</p>
 *
 * @author 雨夜终端
 */
public interface Subscription extends AutoCloseable {

    /**
     * 订阅的主题。
     *
     * @return 主题
     */
    String topic();

    /**
     * 是否仍在接收消息。
     *
     * @return 已关闭或被实现内部终止时返回 {@code false}
     */
    boolean isActive();

    /**
     * 取消订阅并释放资源。
     */
    @Override
    void close();
}
