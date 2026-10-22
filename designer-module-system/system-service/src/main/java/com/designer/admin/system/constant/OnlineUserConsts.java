package com.designer.admin.system.constant;

/**
 * 在线用户（Sa-Token 会话）键名。
 *
 * <p>这些键由登录时写入、在线用户列表读取，分属 {@code AuthServiceImpl} 与
 * {@code OnlineUserServiceImpl}。键名写错不会有任何编译期提示 —— 在线列表只会显示空白，
 * 所以必须由同一份常量保证两侧一致。</p>
 *
 * @author 雨夜终端
 */
public final class OnlineUserConsts {

    /** 会话键：登录 IP */
    public static final String TOKEN_LOGIN_IP = "loginIp";

    /** 会话键：浏览器标识 */
    public static final String TOKEN_BROWSER = "browser";

    /** 会话键：操作系统标识 */
    public static final String TOKEN_OS = "os";

    /** 会话键：登录时间（毫秒时间戳） */
    public static final String TOKEN_LOGIN_TIME = "loginTime";

    private OnlineUserConsts() {
    }
}
