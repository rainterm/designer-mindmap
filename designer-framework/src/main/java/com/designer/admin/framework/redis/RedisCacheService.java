package com.designer.admin.framework.redis;

import lombok.extern.slf4j.Slf4j;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.data.cache.impl.JsonSerializer;

import java.lang.reflect.Type;

/**
 * {@code @Cache} / {@code @CacheRemove} 的 Redis 实现。
 *
 * <p><b>为什么必须提供</b>：{@code solon-cache} 在容器里找不到 {@link CacheService} 时会
 * 兜底 {@code LocalCacheService}（进程内 Map）。单机看不出问题，多实例时
 * A 实例的 {@code @CacheRemove} 清不掉 B 实例里的旧值，只能等 {@code seconds} 到期 ——
 * 表现为「后台改了设置，前台要过一会儿才生效，而且刷新几次可能又变回去」。
 * 缓存与 Sa-Token 会话一样，属于必须跨实例一致的状态。</p>
 *
 * <p><b>怎么被选中</b>：{@code solon-cache} 的 {@code CacheServiceWrapConsumer} 会订阅
 * 所有 {@link CacheService} bean，把 {@code typed = true} 的实例同时注册为默认服务（{@code ""}），
 * 正好对应注解上 {@code service} 的默认值。所以这里只声明 {@code @Component(typed = true)}，
 * 不需要额外配置。</p>
 *
 * <p><b>可用性</b>：缓存是旁路，Redis 抖动时 fail-open —— 读失败当作未命中、写失败只记日志，
 * 让业务回源数据库，而不是把一次缓存故障放大成接口 500。</p>
 *
 * @author 雨夜终端
 */
@Component(typed = true)
@Slf4j
public class RedisCacheService implements CacheService {

    /** 注解未指定 seconds 时的兜底时长，与 {@code LocalCacheService} 的默认值保持一致 */
    private static final int DEFAULT_SECONDS = 30;

    @Inject
    private RedisClient redisClient;

    @Override
    public void store(String key, Object obj, int seconds) {
        if (obj == null) {
            // CachePut 可能传 null；存 "null" 会让后续 get 命中一个假值
            remove(key);
            return;
        }
        try {
            String body = JsonSerializer.typedInstance.serialize(obj);
            int ttl = seconds > 0 ? seconds : DEFAULT_SECONDS;
            // expire 必须在 set 之前：redisx 的 expire 只是给「紧随其后的写命令」带上 TTL
            redisClient.open(s -> s.key(cacheKey(key)).expire(ttl).set(body));
        } catch (Exception e) {
            log.warn("[cache] 写入失败 key={}", key, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Type type) {
        try {
            String body = redisClient.openAndGet(s -> s.key(cacheKey(key)).get());
            if (body == null) {
                return null;
            }
            // typed 序列化会写入类名，接口/泛型返回值也能还原成原类型
            return (T) JsonSerializer.typedInstance.deserialize(body, type);
        } catch (Exception e) {
            log.warn("[cache] 读取失败 key={}", key, e);
            return null;
        }
    }

    @Override
    public void remove(String key) {
        try {
            redisClient.open(s -> s.key(cacheKey(key)).delete());
        } catch (Exception e) {
            log.warn("[cache] 删除失败 key={}", key, e);
        }
    }

    private static String cacheKey(String key) {
        return RedisKeyConsts.CACHE_PREFIX + key;
    }
}
