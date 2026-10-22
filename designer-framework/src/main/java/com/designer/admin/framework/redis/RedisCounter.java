package com.designer.admin.framework.redis;

import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * 固定窗口计数器（Redis INCR + 首次写入时设 TTL）。
 *
 * <p><b>为什么单独抽一个类，而不是各处写 {@code incrWithTtl}</b>：
 * {@code redisx} 的 {@code expire(seconds)} **不执行任何 Redis 命令**，
 * 它只是把毫秒数记在当前会话对象上，真正的 {@code PEXPIRE} 由「紧跟在后面的那条写命令」
 * （{@code set} / {@code incr} / {@code hashSet} …内部调用的 {@code expirePush()}）带出去。
 * 一旦把 {@code expire} 写在写命令**之后**，这个字段就再也没人消费 —— 于是键被创建出来却**永不过期**。</p>
 *
 * <pre>
 * // ✗ 曾经三处都这么写，看着像对的，其实 TTL 从来没生效：
 * //   incr 之前 _milliseconds 还是 0，expirePush() 空转；expire 之后又没有任何写命令
 * long c = s.key(key).incr();
 * if (c == 1) {
 *     s.key(key).expire(ttlSeconds);
 * }
 *
 * // ✗ 改成 expire 在前也不行：incr() 每次都会 expirePush()，
 * //   等于每条请求都把 TTL 续满，窗口永远不结束（退化成滑动窗口，与阈值设定不符）
 * s.key(key).expire(ttlSeconds).incr();
 * </pre>
 *
 * <p><b>后果</b>：计数只增不减，一旦超过阈值，该 IP 就被**永久**拉黑 ——
 * 重启进程无效（键在 Redis 里，而且有持久化），表现为「报错很久之后依然不解除」。</p>
 *
 * <p><b>这里的实现</b>用 Lua 把「判断窗口 → 自增」做成一次原子操作，
 * 语义与原意图一致：</p>
 * <ul>
 *   <li>窗口不存在（{@code TTL == -2}）→ 建新窗口，值 1，TTL = {@code ttlSeconds}；</li>
 *   <li>窗口存在（{@code TTL > 0}）→ 只自增，**不改 TTL**（固定窗口，不会被刷请求续期）；</li>
 *   <li>键存在但没有 TTL（{@code TTL == -1}）→ 视为脏数据（旧实现留下的），重置成新窗口。
 *       这条是**自愈**：线上已经卡死的键不用人工 DEL，部署后自动恢复。</li>
 * </ul>
 *
 * <p>⚠️ 固定窗口的边界锚在「窗口内第一次请求」，不是每条请求都往后推：
 * 阈值 20/分钟时，第 21 次请求会失败，但窗口结束后立刻可用。</p>
 *
 * @author 雨夜终端
 */
@Component
public class RedisCounter {

    /**
     * KEYS[1] = 计数键，ARGV[1] = 窗口秒数。
     *
     * <p>{@code TTL < 0} 同时覆盖 -2（键不存在）与 -1（键存在但无过期时间），
     * 两种情况都按「新窗口」处理，因此不需要额外的 if 分支。
     * 用 {@code SET key 1 EX ttl} 而不是 {@code INCR}+{@code EXPIRE}，
     * 避免「先写值再设过期」中间可能残留无 TTL 键的老问题。</p>
     */
    private static final String INCR_WITH_TTL_LUA =
            "local ttl = redis.call('TTL', KEYS[1]) "
                    + "if ttl < 0 then "
                    + "  redis.call('SET', KEYS[1], 1, 'EX', ARGV[1]) "
                    + "  return 1 "
                    + "end "
                    + "return redis.call('INCR', KEYS[1])";

    @Inject
    private RedisClient redisClient;

    /**
     * 窗口内计数 +1，返回自增后的值（首次调用返回 1）。
     *
     * @param key        计数键
     * @param ttlSeconds 窗口长度（秒）；从窗口内第一次请求开始计
     */
    public long incrWithTtl(String key, int ttlSeconds) {
        Object result = redisClient.openAndGet(s ->
                s.jedis().eval(INCR_WITH_TTL_LUA, 1, key, String.valueOf(ttlSeconds)));
        if (result instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalStateException("Redis INCR 返回值异常：" + result);
    }

    /**
     * 计数键的剩余存活秒数，用于把「还要等多久」写进提示语。
     *
     * @return 剩余秒数；键不存在返回 {@code -2}、键存在但无过期时间返回 {@code -1}（同 Redis TTL 语义）
     */
    public long ttlSeconds(String key) {
        return redisClient.openAndGet(s -> s.jedis().ttl(key));
    }
}
