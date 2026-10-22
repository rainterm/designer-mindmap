package com.designer.admin.framework.message;

/**
 * 消息渠道类型标识。
 *
 * <p>类型值是渠道注册表（{@code MessageChannelRegistry}）的键，也是数据库
 * {@code sys_message_config.channel_type} 存的值。provider 声明一次、注册表按它索引、
 * 后台配置按它检索 —— 任何一处写成另一个字面量，渠道都会「注册了但读不到配置」。
 * 因此由这个常量类统一定义，provider 与业务侧都从这里引用。</p>
 *
 * @author 雨夜终端
 */
public final class MessageChannelConsts {

    /** 站内信渠道 */
    public static final String TYPE_INBOX = "inbox";

    /** 邮件渠道 */
    public static final String TYPE_EMAIL = "email";

    private MessageChannelConsts() {
    }
}
