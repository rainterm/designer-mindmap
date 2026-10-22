package com.designer.admin.framework.mq;

/**
 * 消息总线操作失败。
 *
 * <p>继承 {@link RuntimeException}：发布失败通常发生在一次业务操作的旁路
 * （如发通知后广播刷新），调用方需要自行决定「降级」还是「让业务失败」，
 * 强制受检异常会逼着每个调用点写 try-catch，反而容易被吞掉。</p>
 *
 * <p>与业务异常 {@code BizException} 区分开：这是基础设施故障，
 * 不该被全局异常处理器翻译成「参数错误」之类的用户提示。</p>
 *
 * @author 雨夜终端
 */
public class MessageBusException extends RuntimeException {

    public MessageBusException(String message) {
        super(message);
    }

    public MessageBusException(String message, Throwable cause) {
        super(message, cause);
    }
}
