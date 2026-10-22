package com.designer.admin.framework.redis;

/**
 * Redis 键的通用约定：分段分隔符与限流键的固定片段。
 *
 * <p>业务键的字面量不集中在这里，而是由各模块自己的 {@code *RedisKeyConsts} 维护：
 * framework 不认识 system / demo 的业务语义，把业务键塞进来会形成反向依赖。
 * 但「怎么拼」必须统一 —— 分隔符与限流命名空间一旦各写各的，键就会长得五花八门，
 * 排查问题时没法按前缀批量检索，也没法判断某个键归谁所有。</p>
 *
 * <p>键名一旦上线就是事实上的数据契约：改名等于让旧键变成永远不会再被读取的垃圾，
 * 因此这里的常量只允许新增，不允许悄悄改值。</p>
 *
 * @author 雨夜终端
 */
public final class RedisKeyConsts {

    /** 键的分段分隔符 */
    public static final String SEPARATOR = ":";

    /**
     * 限流键前缀，完整形状：{@code rate:{name}:{维度主体}:{count}x{window}}。
     *
     * <p>与业务自己的计数键分开命名空间：限流键由框架统一回收（固定窗口到期即消失），
     * 而业务键的生命周期由业务决定，混在一起会让「谁该过期」变得难以判断。</p>
     */
    public static final String RATE_LIMIT_PREFIX = "rate:";

    /**
     * {@code @Cache} / {@code @CacheRemove} 的缓存键前缀，完整形状：{@code cache:{注解生成的键}}。
     *
     * <p>框架只负责算键、不会加命名空间；不隔离的话，缓存键会与限流键、
     * 业务计数器共用一个平坦名字空间，排查时无法按前缀区分归属。</p>
     */
    public static final String CACHE_PREFIX = "cache:";

    /** 限流维度主体前缀：按客户端 IP 计数 */
    public static final String RATE_SUBJECT_IP_PREFIX = "ip:";

    /** 限流维度主体前缀：按登录用户计数 */
    public static final String RATE_SUBJECT_USER_PREFIX = "user:";

    /** 限流维度主体前缀：按请求头取值计数 */
    public static final String RATE_SUBJECT_HEADER_PREFIX = "hdr:";

    /** 限流维度主体：GLOBAL 维度下所有请求共享同一个桶 */
    public static final String RATE_SUBJECT_GLOBAL = "all";

    private RedisKeyConsts() {
    }
}
