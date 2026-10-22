package com.designer.admin.framework.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import redis.clients.jedis.params.SetParams;

/**
 * AJ-Captcha 的验证码存储，落到 Redis。
 *
 * <p><b>为什么必须自己实现一个</b>：{@code captcha-solon-plugin} 的默认存储
 * （{@code cache-type: local}）是**进程内内存**（{@code CaptchaCacheServiceMemImpl}）。
 * 单机跑没问题，一旦多实例部署就必然随机失败：A 实例通过 {@code /captcha/get} 下发验证码、
 * 把答案留在自己的堆里，用户拖完滑块后的 {@code /captcha/check} 被 nginx 轮询到 B 实例，
 * B 找不到这个 token，直接判失败。进程重启同理（验证码有效期内重启就全失效）。
 * 这和 Sa-Token 会话必须走 Redis 是同一类问题。</p>
 *
 * <p><b>怎么被装配</b>：{@code CaptchaServiceFactory} 用 {@code ServiceLoader} 装载所有
 * {@link CaptchaCacheService}，并放进一张以 {@link #type()} 为名的表里，
 * 之后按配置项 {@code aj.captcha.cache-type} 查表取用。
 * 所以这个类不是 Solon 容器里的 bean，而是在
 * {@code src/main/resources/META-INF/services/com.anji.captcha.service.CaptchaCacheService}
 * 里声明、由 JDK SPI 反射实例化的 —— 这也解释了下面为什么必须**惰性**取 {@link RedisClient}。</p>
 *
 * <p><b>为什么 RedisClient 是懒加载的</b>：SPI 用无参构造直接 new，绕过了 Solon 的依赖注入，
 * 拿不到 {@code @Inject}；而 {@code @Bean} 注册又来不及 ——
 * {@code AbstractCaptchaService#init} 在**构造限流器**时就会调用
 * {@code CaptchaServiceFactory.getCache("redis")}，这一步发生在容器启动期间，
 * 各 {@code @Configuration} 的装配顺序并不保证 {@link RedisClient} 已经就绪。
 * 好在 {@code init} 只是把缓存对象存下来（本身不读写 Redis），
 * 第一次真正读写发生在请求线程里，那时容器早已就绪。故此处延迟到首次使用再取，
 * 用 double-check 保证只解析一次、且不引入每次调用的同步开销。</p>
 *
 * <p><b>与 {@code RedisCounter} 的区别</b>：两者的 Lua 看着像，语义并不相同，
 * 故意没有合并。{@code RedisCounter} 是「固定窗口」——窗口内只自增、绝不续期；
 * 而这里的 {@link #increment} 服务于 AJ-Captcha 的频控：
 * {@code DefaultLimitHandler} 的模式是「先 {@link #set} 建键（带 TTL）再 increment」，
 * 因此 TTL 已由 {@code set} 建立，{@code increment} 只做纯自增即可；
 * 仅在键不存在时（未经 set 直接自增）才补一个 TTL，防止漏出永不过期的计数键。</p>
 *
 * @author 雨夜终端
 */
@Slf4j
public class RedisCaptchaCacheServiceImpl implements CaptchaCacheService {

    /**
     * KEYS[1] = 计数键，ARGV[1] = TTL 秒。
     *
     * <p>整体放进 Lua 是为了原子：{@code INCR} 与「补 TTL」之间不能有窗口，
     * 否则进程在中间挂掉就留下一个无过期时间的计数键。</p>
     */
    private static final String INCR_LUA =
            "local v = redis.call('INCR', KEYS[1]) "
                    + "if v == 1 then "
                    + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
                    + "end "
                    + "return v";

    private volatile RedisClient redisClient;

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        // 用 SET + EX 而不是 redisx 的 expire(...).set(...)：语义显式，
        // 也不必依赖「expire 只记在本会话、由下一条写命令带出去」这个隐晦约定。
        // Jedis 7.3 起 SETEX 已过时，官方建议改用 SET + SetParams.ex()。
        redisClient().open(s -> s.jedis().set(key, value, SetParams.setParams().ex(expiresInSeconds)));
    }

    @Override
    public boolean exists(String key) {
        Boolean exists = redisClient().openAndGet(s -> s.jedis().exists(key));
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void delete(String key) {
        redisClient().open(s -> s.jedis().del(key));
    }

    @Override
    public String get(String key) {
        return redisClient().openAndGet(s -> s.jedis().get(key));
    }

    /** 需与 {@code aj.captcha.cache-type} 的取值一致，否则查不到本实现。 */
    @Override
    public String type() {
        return "redis";
    }

    @Override
    public Long increment(String key, long expiresInSeconds) {
        Object result = redisClient().openAndGet(s ->
                s.jedis().eval(INCR_LUA, 1, key, String.valueOf(expiresInSeconds)));
        if (result instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalStateException("Redis INCR 返回值异常：" + result);
    }

    private RedisClient redisClient() {
        RedisClient client = redisClient;
        if (client == null) {
            synchronized (this) {
                client = redisClient;
                if (client == null) {
                    client = Solon.context().getBean(RedisClient.class);
                    if (client == null) {
                        throw new IllegalStateException(
                                "验证码存储需要 RedisClient，但容器中不存在该 bean；"
                                        + "请确认 RedisClientConfig 已被扫描");
                    }
                    redisClient = client;
                    log.info("验证码存储已接管 AJ-Captcha 的缓存接口（cache-type=redis）");
                }
            }
        }
        return client;
    }
}
