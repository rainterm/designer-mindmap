package com.designer.admin.framework.satoken;

import com.designer.admin.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.redisx.RedisClient;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 权限快照缓存（Redis，跨实例共享）。
 *
 * <p><b>为什么用 Redis 而不是进程内 Map</b>：权限是「鉴权时才读、变更时写」的状态，
 * 和 Sa-Token 会话一样必须跨实例一致。进程内缓存多副本下会变成
 * 「A 实例改了角色，请求落到 B 实例仍按旧权限放行」—— 看起来是缓存没刷新，实际是越权。</p>
 *
 * <p><b>失效策略</b>：写侧主动删键（见 system 模块的 {@code PermissionInvalidator}），
 * TTL 只是兜底 —— 覆盖绕过应用的直连改库、以及万一漏掉的失效点；
 * 到期后回源一次数据库，不是主要依赖。TTL 刻意压到 10 分钟：
 * 缓存与数据库之间仍存在「回源读到提交前数据、又在删键之后写回」的窄窗口，
 * 无法用删键彻底消除，只能靠较短的 TTL 把最坏情况（权限撤销延迟生效）限制住。</p>
 *
 * <p><b>可用性</b>：缓存是旁路，Redis 异常时 fail-open —— 读失败当作未命中、
 * 写失败只记日志，让调用方回源数据库，而不是把一次缓存故障放大成「所有接口 401」。
 * 反过来，删失败只记日志是刻意的：旧值最多残留一个 TTL，比让角色修改接口整体失败更可接受。</p>
 *
 * <p>键形状：{@code perm:user:{userId}}。
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class PermissionCache {

    /** 权限快照键前缀，完整形状：{@code perm:user:{userId}} */
    private static final String KEY_PREFIX = "perm:user:";

    /** 未配置时的兜底存活时间（秒）：10 分钟（同时是缓存回写竞争的最坏失效窗口） */
    private static final int DEFAULT_TTL_SECONDS = 600;

    /** 配置项：{@code <=0} 表示关闭缓存（排查问题时用，等价于改造前行为） */
    private static final String TTL_CONFIG_KEY = "designer.permission.cache-seconds";

    @Inject
    private RedisClient redisClient;

    private int ttlSeconds = DEFAULT_TTL_SECONDS;

    @Init
    public void init() {
        ttlSeconds = Solon.cfg().getInt(TTL_CONFIG_KEY, DEFAULT_TTL_SECONDS);
    }

    /**
     * 读取快照。
     *
     * @param userId 用户 ID
     * @return 命中返回快照；未命中 / 缓存关闭 / Redis 异常返回 {@code null}
     */
    public PermissionSnapshot get(Long userId) {
        if (!enabled() || userId == null) {
            return null;
        }
        try {
            String body = redisClient.openAndGet(s -> s.key(key(userId)).get());
            if (body == null || body.isBlank()) {
                return null;
            }
            return JsonUtil.fromJson(body, PermissionSnapshot.class);
        } catch (Exception e) {
            // fail-open：按未命中处理，回源数据库
            log.warn("[perm-cache] 读取失败 userId={}", userId, e);
            return null;
        }
    }

    /**
     * 写入快照（带 TTL）。
     *
     * @param userId   用户 ID
     * @param snapshot 快照，不能为空
     */
    public void put(Long userId, PermissionSnapshot snapshot) {
        if (!enabled() || userId == null || snapshot == null) {
            return;
        }
        try {
            String body = JsonUtil.toJson(snapshot);
            // expire 必须在 set 之前：redisx 的 expire 只是给「紧随其后的写命令」带上 TTL
            redisClient.open(s -> s.key(key(userId)).expire(ttlSeconds).set(body));
        } catch (Exception e) {
            log.warn("[perm-cache] 写入失败 userId={}", userId, e);
        }
    }

    /**
     * 批量删除快照（主动失效）。
     *
     * @param userIds 用户 ID 集合，允许为空
     */
    public void evict(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            if (userId != null) {
                keys.add(key(userId));
            }
        }
        if (keys.isEmpty()) {
            return;
        }
        try {
            // 一次 DEL 多键：单个角色的用户数可能很大，逐键删除会把一次请求放大成 N 次往返
            redisClient.open(s -> s.deleteKeys(keys));
        } catch (Exception e) {
            log.warn("[perm-cache] 失效失败 userIds={}", keys, e);
        }
    }

    /** 缓存是否启用（TTL <= 0 时关闭，便于线上排查「问题在缓存还是数据库」） */
    private boolean enabled() {
        return ttlSeconds > 0;
    }

    private static String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
