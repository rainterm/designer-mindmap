package com.designer.admin.framework.message;

import cn.dev33.satoken.stp.StpUtil;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.mq.MessageBusManager;
import com.designer.admin.framework.mq.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.noear.snack4.ONode;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.util.RunUtil;
import org.noear.solon.web.sse.SseEmitter;
import org.noear.solon.web.sse.SseEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 站内消息 SSE 连接中心（本机维护连接，跨实例经消息总线广播）
 *
 * <p><b>问题</b>：SSE 是长连接，连接只落在某一个实例上。发送通知的请求可能被负载均衡到
 * 另一个实例，如果只遍历本进程的 {@code sessions}，消息就永远到不了对端节点上的浏览器 ——
 * 表现为「有时能收到、刷新几次才收到」，多副本时尤其明显。</p>
 *
 * <p><b>做法</b>：本实例订阅消息总线的一个 topic，发送方只 publish 一次，
 * 所有实例（包括自己）都会收到，各自把消息写到<b>本机</b>持有的连接上。
 * 因此这里不做「本机直接推送 + 广播」—— 广播会把消息回传给发布者，再本地推一次就是重复投递
 * （前端会收到两条通知）。</p>
 *
 * <p><b>与 MQ 实现的解耦</b>：本类只依赖 {@link MessageBusManager}，
 * 不关心底层是 Redis Pub/Sub、RabbitMQ 还是单机内存（见 framework 的 {@code mq} 包）。
 * 换 MQ 只改 {@code designer.mq.type}，这里不受影响。</p>
 *
 * <p><b>降级</b>：发布失败时退回只推本机。跨节点必然收不到，但至少同节点用户不丢消息；
 * 这比整体静默失败更可取。</p>
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class NoticeSseHub {

    /** 主题配置项 */
    private static final String TOPIC_CONFIG_KEY = "designer.sse.topic";

    /**
     * 默认主题名。
     *
     * <p>主题名一旦上线就是契约：改名等于让新旧实例各发各的，滚动发布期间会丢消息，
     * 所以只允许通过配置在部署时整体切换，不要随手改这个默认值。</p>
     */
    private static final String DEFAULT_TOPIC = "designer:sse:notice";

    /** SSE 心跳间隔（毫秒）：需小于前端重连阈值 */
    private static final long HEARTBEAT_INTERVAL_MILLIS = 25_000L;

    /** userId -> emitters（同用户多标签页） */
    private final Map<Long, CopyOnWriteArraySet<SseEmitter>> sessions = new ConcurrentHashMap<>();

    /**
     * emitter -> 建立连接时使用的 token。
     *
     * <p>心跳时用它复核登录态：用户被踢下线、主动登出或 token 过期后，
     * 长连接不会自己断开，不校验就会继续给他推送消息。</p>
     */
    private final Map<SseEmitter, String> tokens = new ConcurrentHashMap<>();

    @Inject
    private MessageBusManager messageBus;

    private String topic = DEFAULT_TOPIC;

    private Subscription subscription;

    @Init(index = 2)
    public void init() {
        topic = Solon.cfg().get(TOPIC_CONFIG_KEY, DEFAULT_TOPIC);
        // 先订阅再开心跳：否则启动瞬间建立的长连接收不到广播
        subscription = messageBus.subscribe(topic, this::handleMessage);

        // 复用 Solon 的全局调度线程池：随 SolonApp 一起关闭，不需要自己管生命周期
        RunUtil.scheduleAtFixedRate(this::pingAll, HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS);
    }

    @Destroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
        }
        for (Map.Entry<Long, CopyOnWriteArraySet<SseEmitter>> entry : sessions.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                remove(entry.getKey(), emitter);
            }
        }
        sessions.clear();
        tokens.clear();
    }

    /**
     * 建立订阅：不超时，由心跳 + 前端重连保活。
     *
     * @param userId 登录用户 ID
     * @param token  建立连接所用的 token，心跳时复核登录态；为空则不校验
     */
    public SseEmitter open(Long userId, String token) {
        final SseEmitter[] box = new SseEmitter[1];
        SseEmitter emitter = new SseEmitter(-1L)
                .onCompletion(() -> remove(userId, box[0]))
                .onError(e -> remove(userId, box[0]))
                .onInited(s -> {
                    box[0] = s;
                    // onCompletion 可能先于 onInited 触发（客户端建连即断开），此时不能再登记
                    if (s.isCompleted()) {
                        return;
                    }
                    if (token != null && !token.isBlank()) {
                        tokens.put(s, token);
                    }
                    sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(s);
                    try {
                        s.send(new SseEvent().name("connected").data("ok").reconnectTime(3000L));
                    } catch (Exception e) {
                        log.debug("SSE 初始推送失败 userId={}", userId, e);
                        remove(userId, s);
                    }
                });
        box[0] = emitter;
        return emitter;
    }

    /**
     * 推送给指定用户（跨实例）。
     *
     * @param userIds   目标用户
     * @param eventName 事件名
     * @param json      事件数据
     */
    public void pushToUsers(Iterable<Long> userIds, String eventName, String json) {
        if (userIds == null) {
            return;
        }
        List<Long> targets = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId != null) {
                targets.add(userId);
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        Map<String, Object> message = new LinkedHashMap<>(4);
        message.put("users", targets);
        message.put("event", eventName);
        message.put("data", json);
        publish(message, () -> targets.forEach(userId -> deliverToUser(userId, eventName, json)));
    }

    /**
     * 推送给所有在线用户（跨实例）。
     *
     * @param eventName 事件名
     * @param json      事件数据
     */
    public void pushToAllOnline(String eventName, String json) {
        Map<String, Object> message = new LinkedHashMap<>(3);
        message.put("all", true);
        message.put("event", eventName);
        message.put("data", json);
        publish(message, () -> deliverToAllOnline(eventName, json));
    }

    /**
     * 推送给单个用户（跨实例）。
     *
     * @param userId    目标用户
     * @param eventName 事件名
     * @param json      事件数据
     */
    public void pushToUser(Long userId, String eventName, String json) {
        if (userId == null) {
            return;
        }
        pushToUsers(List.of(userId), eventName, json);
    }

    /**
     * 发布到主题；失败时只推本机。
     *
     * @param message       消息体
     * @param localFallback 降级动作
     */
    private void publish(Map<String, Object> message, Runnable localFallback) {
        try {
            messageBus.publish(topic, JsonUtil.toJson(message));
        } catch (Exception e) {
            log.warn("SSE 消息发布失败，降级为仅推送本实例 topic={} bus={}", topic, messageBus.type(), e);
            try {
                localFallback.run();
            } catch (Exception ex) {
                log.warn("SSE 本机降级推送失败", ex);
            }
        }
    }

    /**
     * 处理总线消息：只做解析，投递交给 I/O 线程池。
     *
     * <p>回调运行在 MQ 的接收线程上，是所有消息共用的：如果在这里同步写所有连接，
     * 一个慢客户端（TCP 缓冲区写满）就会拖住整个实例的消息投递，其它用户跟着延迟。</p>
     *
     * @param ignoredTopic 主题（本实例只订阅一个 topic，无需区分）
     * @param payload      消息体
     */
    private void handleMessage(String ignoredTopic, String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            ONode root = ONode.ofJson(payload);
            String event = root.get("event").getString();
            String data = root.get("data").getString();
            if (event == null || data == null) {
                return;
            }
            RunUtil.asyncAndTry(() -> dispatch(root, event, data));
        } catch (Exception e) {
            log.warn("SSE 消息解析失败 payload={}", abbreviate(payload), e);
        }
    }

    private void dispatch(ONode root, String event, String data) {
        if (root.get("all").getBoolean(false)) {
            deliverToAllOnline(event, data);
            return;
        }

        ONode users = root.get("users");
        if (!users.isArray()) {
            return;
        }
        for (ONode item : users.getArray()) {
            Long userId = item.getLong();
            if (userId != null) {
                deliverToUser(userId, event, data);
            }
        }
    }

    private void deliverToAllOnline(String eventName, String json) {
        for (Long userId : sessions.keySet()) {
            deliverToUser(userId, eventName, json);
        }
    }

    private void deliverToUser(Long userId, String eventName, String json) {
        CopyOnWriteArraySet<SseEmitter> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        SseEvent event = new SseEvent().name(eventName).data(json).reconnectTime(3000L);
        for (SseEmitter emitter : set) {
            try {
                emitter.send(event);
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    private void pingAll() {
        // 定时任务里抛出的异常会取消后续调度，这里整体兜一层
        try {
            SseEvent ping = new SseEvent().name("ping").data(String.valueOf(System.currentTimeMillis()));
            for (Map.Entry<Long, CopyOnWriteArraySet<SseEmitter>> entry : sessions.entrySet()) {
                Long userId = entry.getKey();
                for (SseEmitter emitter : entry.getValue()) {
                    // 登出 / 被踢 / token 过期后，长连接必须主动回收；否则连接仍在，消息照推
                    if (!isTokenValid(userId, emitter)) {
                        remove(userId, emitter);
                        continue;
                    }
                    try {
                        emitter.send(ping);
                    } catch (Exception ex) {
                        remove(userId, emitter);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("SSE 心跳推送失败", ex);
        }
    }

    /**
     * 心跳时复核 token。
     *
     * <p>连接建立后浏览器不会再发任何请求，Sa-Token 的活跃续期与踢人逻辑都感知不到它，
     * 因此只能由服务端周期性确认登录态。Redis 异常时按「有效」处理，避免故障期间误踢全部在线用户。</p>
     */
    private boolean isTokenValid(Long userId, SseEmitter emitter) {
        String token = tokens.get(emitter);
        if (token == null) {
            return true;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null && String.valueOf(userId).equals(String.valueOf(loginId));
        } catch (Exception e) {
            log.debug("SSE token 校验失败 userId={}", userId, e);
            return true;
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        if (userId == null || emitter == null) {
            return;
        }
        tokens.remove(emitter);
        CopyOnWriteArraySet<SseEmitter> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        set.remove(emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignore
        }
        if (set.isEmpty()) {
            sessions.remove(userId, set);
        }
    }

    private static String abbreviate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
